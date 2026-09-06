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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layout-specific reader for HDFC Bank Credit Card PDF statements (like Swiggy HDFC).
 */
@Slf4j
@Component
public class HdfcCreditCardPdfStatementParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Matches: "07/11/2025 | 12:36 Narration..."
    // Also matches variations without the pipe or time, or 2-digit years
    private static final Pattern ROW_START_PATTERN = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2,4})\\s*(?:\\|?\\s*\\d{2}:\\d{2}\\s+)?(.*)");

    // Matches: "Narration... ₹ 319.00 ."
    // Or: "Narration... ₹ 319.00 Cr ."
    // Allows missing symbols or missing dots at the end
    private static final Pattern ROW_END_PATTERN = Pattern.compile("^(.*?)\\s+(?:(?:\\u20B9|Rs\\.?|INR)\\s*)?([\\d,]+\\.\\d{2})\\s*(Cr|Dr)?(?:\\s+\\S+)?\\s*$");

    public StatementParseResult parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    public StatementParseResult parse(InputStream inputStream, String fileName, String password) {
        List<RawTransactionRecord> records = new ArrayList<>();
        List<InvalidRowReport> invalidRows = new ArrayList<>();

        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            return new StatementParseResult(List.of(), List.of());
        }

        try (PDDocument document = (password != null && !password.isBlank())
                ? Loader.loadPDF(bytes, password)
                : Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");

            String currentDateStr = null;
            StringBuilder currentNarration = new StringBuilder();
            String currentRestOfLine = null;
            int currentTransactionRowNum = 0;
            int currentRowNum = 0;

            for (String line : lines) {
                line = line.trim();
                currentRowNum++;
                if (line.isEmpty()) continue;

                Matcher startMatcher = ROW_START_PATTERN.matcher(line);
                if (startMatcher.find()) {
                    // Flush previous record
                    if (currentDateStr != null) {
                        processRecord(currentTransactionRowNum, currentDateStr, currentNarration.toString(), currentRestOfLine, records, invalidRows);
                    }

                    // Start new record
                    currentTransactionRowNum = currentRowNum;
                    currentDateStr = startMatcher.group(1); // the date part
                    currentRestOfLine = startMatcher.group(2);
                    currentNarration = new StringBuilder();
                } else {
                    // Continuation line for narration (only if we've started a record)
                    if (currentDateStr != null) {
                        if (currentNarration.length() > 0) {
                            currentNarration.append(" ");
                        }
                        currentNarration.append(line);
                    }
                }
            }

            // Flush the last record
            if (currentDateStr != null) {
                processRecord(currentTransactionRowNum, currentDateStr, currentNarration.toString(), currentRestOfLine, records, invalidRows);
            }

        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            throw new com.finapse.exception.EncryptedPdfException("This PDF statement is password-protected. Please provide the statement password.");
        } catch (Exception e) {
            log.warn("HDFC Credit Card layout did not apply to PDF {}: {}", fileName, e.getMessage());
            return new StatementParseResult(List.of(), List.of());
        }

        return new StatementParseResult(records, invalidRows);
    }

    private void processRecord(int rowNum, String dateStr, String appendedNarration, String restOfFirstLine,
                               List<RawTransactionRecord> records, List<InvalidRowReport> invalidRows) {
        try {
            Matcher endMatcher = ROW_END_PATTERN.matcher(restOfFirstLine);
            if (!endMatcher.find()) {
                invalidRows.add(new InvalidRowReport(rowNum, "Could not match amounts at the end of the line", restOfFirstLine));
                return;
            }

            String narrationPart = endMatcher.group(1).trim();
            String amountStr = endMatcher.group(2).trim();
            String markerStr = endMatcher.group(3); // "Cr" if present

            // Combine narration
            String fullNarration = narrationPart;
            if (!appendedNarration.isEmpty()) {
                fullNarration = fullNarration + " " + appendedNarration;
            }
            if (fullNarration.length() > 500) {
                fullNarration = fullNarration.substring(0, 500);
            }

            LocalDate transactionDate = parseDate(dateStr);
            if (transactionDate == null) {
                throw new DateTimeParseException("Invalid date", dateStr, 0);
            }
            BigDecimal amount = new BigDecimal(amountStr.replace(",", ""));
            
            TransactionDirection direction = TransactionDirection.DEBIT;
            if (markerStr != null && markerStr.equalsIgnoreCase("Cr")) {
                direction = TransactionDirection.CREDIT;
            } else {
                String upperDesc = fullNarration.toUpperCase();
                if (upperDesc.contains("REVERSAL") || upperDesc.contains("CASHBACK") || upperDesc.contains("REFUND")) {
                    direction = TransactionDirection.CREDIT;
                }
            }

            String rawDebit = direction == TransactionDirection.DEBIT ? amount.toString() : "";
            String rawCredit = direction == TransactionDirection.CREDIT ? amount.toString() : "";

            records.add(new RawTransactionRecord(
                    rowNum,
                    transactionDate,
                    null,
                    fullNarration,
                    amount,
                    direction,
                    rawDebit,
                    rawCredit,
                    dateStr
            ));

        } catch (DateTimeParseException e) {
            invalidRows.add(new InvalidRowReport(rowNum, "Invalid date format", dateStr));
        } catch (Exception e) {
            invalidRows.add(new InvalidRowReport(rowNum, "Failed to parse record: " + e.getMessage(), restOfFirstLine));
        }
    }

    private LocalDate parseDate(String raw) {
        String cleaned = raw.trim();
        List<DateTimeFormatter> formats = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
        );
        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (Exception ignored) { }
        }
        return null;
    }
}
