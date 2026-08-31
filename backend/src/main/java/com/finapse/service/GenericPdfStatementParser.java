package com.finapse.service;

import com.finapse.dto.RawTransactionRecord;
import com.finapse.dto.StatementParseResult;
import com.finapse.dto.StatementParseResult.InvalidRowReport;
import com.finapse.enums.TransactionDirection;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank-agnostic PDF reader used when no layout-specific parser matches.
 *
 * <p>It looks for lines that begin with a date and end with one or two money
 * amounts. When two trailing amounts are present the second is treated as a
 * running balance and the direction is inferred from the balance delta;
 * otherwise a Dr/Cr marker or a trailing minus decides the direction.
 */
@Slf4j
@Component
public class GenericPdfStatementParser {

    /** dd/MM/yyyy, dd-MM-yy, dd MMM yyyy and the 2-digit-year variants of each. */
    private static final Pattern LEADING_DATE = Pattern.compile(
            "^(\\d{1,2}[/\\-.\\s](?:\\d{1,2}|[A-Za-z]{3})[/\\-.\\s]\\d{2,4})\\s+(.*)$");

    /** One or two money amounts at the end of the line, optional Dr/Cr marker. */
    private static final Pattern TRAILING_AMOUNTS = Pattern.compile(
            "^(.*?)\\s+(-?[\\d,]+\\.\\d{2})\\s*(?:\\((Dr|Cr)\\)|(Dr|Cr))?" +
            "(?:\\s+(-?[\\d,]+\\.\\d{2})\\s*(?:\\((?:Dr|Cr)\\)|Dr|Cr)?)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy"),
            DateTimeFormatter.ofPattern("dd-MMM-yy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"));

    private static final Pattern CREDIT_HINT = Pattern.compile(
            "\\b(CREDIT|CR|REFUND|REVERSAL|CASHBACK|SALARY|INTEREST CREDIT|DEPOSIT|RECEIVED)\\b",
            Pattern.CASE_INSENSITIVE);

    public StatementParseResult parse(InputStream inputStream, String fileName) {
        List<RawTransactionRecord> records = new ArrayList<>();
        List<InvalidRowReport> invalidRows = new ArrayList<>();

        String text;
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(document);
        } catch (Exception e) {
            log.warn("Generic PDF reader could not open {}: {}", fileName, e.getMessage());
            return new StatementParseResult(List.of(), List.of());
        }

        String[] lines = text.split("\\r?\\n");
        List<BigDecimal> balances = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Matcher dateMatcher = LEADING_DATE.matcher(line);
            if (!dateMatcher.matches()) continue;

            String rawDate = dateMatcher.group(1);
            LocalDate date = parseDate(rawDate);
            if (date == null) continue;

            Matcher amountMatcher = TRAILING_AMOUNTS.matcher(dateMatcher.group(2));
            if (!amountMatcher.matches()) continue;

            String description = amountMatcher.group(1).trim();
            if (description.isEmpty()) continue;

            BigDecimal first = money(amountMatcher.group(2));
            String marker = amountMatcher.group(3) != null ? amountMatcher.group(3) : amountMatcher.group(4);
            BigDecimal second = money(amountMatcher.group(5));

            if (first == null || first.signum() == 0) {
                invalidRows.add(new InvalidRowReport(i + 1, "Could not read an amount from this line.", line));
                continue;
            }

            BigDecimal amount = first.abs();
            TransactionDirection direction = resolveDirection(first, marker, second, balances, description);

            balances.add(second);
            records.add(new RawTransactionRecord(
                    i + 1, date, null, description, amount, direction,
                    direction == TransactionDirection.DEBIT ? amount.toPlainString() : "",
                    direction == TransactionDirection.CREDIT ? amount.toPlainString() : "",
                    rawDate));
        }

        return new StatementParseResult(records, invalidRows);
    }

    private TransactionDirection resolveDirection(BigDecimal amount, String marker,
                                                  BigDecimal balance, List<BigDecimal> balances,
                                                  String description) {
        if (marker != null) {
            return marker.equalsIgnoreCase("CR") ? TransactionDirection.CREDIT : TransactionDirection.DEBIT;
        }
        // A running-balance column is the most reliable signal available.
        if (balance != null && !balances.isEmpty()) {
            BigDecimal previous = balances.get(balances.size() - 1);
            if (previous != null) {
                if (previous.subtract(amount.abs()).compareTo(balance) == 0) return TransactionDirection.DEBIT;
                if (previous.add(amount.abs()).compareTo(balance) == 0) return TransactionDirection.CREDIT;
            }
        }
        if (amount.signum() < 0) return TransactionDirection.DEBIT;
        if (CREDIT_HINT.matcher(description).find()) return TransactionDirection.CREDIT;
        return TransactionDirection.DEBIT;
    }

    private LocalDate parseDate(String raw) {
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (Exception ignored) { /* try the next layout */ }
        }
        return null;
    }

    private BigDecimal money(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
