package com.finapse.service;

import com.finapse.dto.StatementParseResult;
import com.finapse.dto.StatementParseResult.InvalidRowReport;
import com.finapse.dto.RawTransactionRecord;
import com.finapse.enums.TransactionDirection;
import com.finapse.exception.InvalidStatementFileException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Responsible exclusively for reading CSV files and extracting raw records.
 * Does NOT classify transactions or make any financial decisions.
 */
@Service
public class CsvImportService implements StatementFileParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }

    // Canonical column name mappings — maps source header variants to internal names
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("date",               "DATE"),
            Map.entry("transaction date",   "DATE"),
            Map.entry("txn date",           "DATE"),
            Map.entry("trans date",         "DATE"),
            Map.entry("txn_date",           "DATE"),
            Map.entry("transaction_date",   "DATE"),
            Map.entry("value date",         "DATE"),
            Map.entry("tran date",          "DATE"),
            Map.entry("posted date",        "POSTED_DATE"),
            Map.entry("posting date",       "POSTED_DATE"),
            Map.entry("description",        "DESCRIPTION"),
            Map.entry("narration",          "DESCRIPTION"),
            Map.entry("particulars",        "DESCRIPTION"),
            Map.entry("transaction details","DESCRIPTION"),
            Map.entry("remarks",            "DESCRIPTION"),
            Map.entry("details",            "DESCRIPTION"),
            Map.entry("summary",            "DESCRIPTION"),
            Map.entry("debit",              "DEBIT"),
            Map.entry("debit amount",       "DEBIT"),
            Map.entry("withdrawal",         "DEBIT"),
            Map.entry("withdrawal (dr)",    "DEBIT"),
            Map.entry("dr",                 "DEBIT"),
            Map.entry("debit(dr)",          "DEBIT"),
            Map.entry("credit",             "CREDIT"),
            Map.entry("credit amount",      "CREDIT"),
            Map.entry("deposit",            "CREDIT"),
            Map.entry("deposit (cr)",       "CREDIT"),
            Map.entry("cr",                 "CREDIT"),
            Map.entry("credit(cr)",         "CREDIT"),
            Map.entry("amount",             "AMOUNT"),
            Map.entry("transaction amount", "AMOUNT"),
            Map.entry("amount (inr)",       "AMOUNT"),
            Map.entry("amt",                "AMOUNT")
    );

    @Override
    public StatementParseResult parse(InputStream inputStream, String fileName) {
        List<RawTransactionRecord> records = new ArrayList<>();
        List<InvalidRowReport> invalidRows = new ArrayList<>();

        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new InvalidStatementFileException("Failed to read CSV file: " + fileName);
        }

        String text = new String(content, StandardCharsets.UTF_8);

        // Auto-detect custom delimiter (~|~, |, \t, ;, ,)
        char delimiter = detectDelimiter(text);

        // Find best header line by scanning lines
        String[] lines = text.split("\r?\n");
        int headerLineIndex = findHeaderLineIndex(lines, delimiter);
        if (headerLineIndex == -1) {
            throw new InvalidStatementFileException(
                    "Could not find a date column. Please ensure your CSV has a column named 'Date', 'Transaction Date', 'Txn Date', or 'Value Date'.");
        }

        // Re-construct CSV content starting from header line
        StringBuilder sb = new StringBuilder();
        for (int i = headerLineIndex; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (InputStreamReader reader = new InputStreamReader(
                new java.io.ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
             CSVParser parser = csvFormat.parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new InvalidStatementFileException("The uploaded CSV does not contain a recognizable header row.");
            }

            ColumnMapping mapping = resolveColumns(headerMap);
            if (mapping.dateCol == null) {
                throw new InvalidStatementFileException("Could not find a date column. Please ensure your CSV has a column named 'Date', 'Transaction Date', 'Txn Date', or 'Value Date'.");
            }
            if (mapping.descriptionCol == null) {
                throw new InvalidStatementFileException("Could not find a description column. Please ensure your CSV has a column named 'Description', 'Narration', or 'Particulars'.");
            }
            if (!mapping.hasAmountColumns()) {
                throw new InvalidStatementFileException("Could not find amount columns. Please ensure your CSV has 'Debit' and 'Credit' columns, or a single 'Amount' column.");
            }

            int rowNumber = headerLineIndex + 2;
            for (CSVRecord csvRecord : parser) {
                String rawLine = csvRecord.toString();
                try {
                    RawTransactionRecord record = parseRow(csvRecord, mapping, rowNumber, rawLine);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    invalidRows.add(new InvalidRowReport(rowNumber, e.getMessage(), rawLine));
                }
                rowNumber++;
            }

        } catch (InvalidStatementFileException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidStatementFileException("Failed to read CSV file: " + fileName);
        }

        if (records.isEmpty() && invalidRows.isEmpty()) {
            throw new InvalidStatementFileException("The CSV file contains no transaction rows.");
        }

        return new StatementParseResult(records, invalidRows);
    }

    private RawTransactionRecord parseRow(CSVRecord csv, ColumnMapping mapping,
                                          int rowNumber, String rawLine) {
        String rawDate = safeGet(csv, mapping.dateCol);
        String rawDesc = safeGet(csv, mapping.descriptionCol);

        // Skip rows that look like sub-headers or summary lines
        if (rawDate == null || rawDate.isBlank() || rawDesc == null || rawDesc.isBlank()) {
            return null;
        }

        LocalDate transactionDate = parseDate(rawDate, rowNumber);
        LocalDate postedDate = null;
        if (mapping.postedDateCol != null) {
            String rawPosted = safeGet(csv, mapping.postedDateCol);
            if (rawPosted != null && !rawPosted.isBlank()) {
                try {
                    postedDate = parseDate(rawPosted, rowNumber);
                } catch (Exception ignored) { /* posted date is optional */ }
            }
        }

        String description = rawDesc.trim();

        // Determine amount and direction
        BigDecimal amount;
        TransactionDirection direction;
        String rawDebit = null;
        String rawCredit = null;

        if (mapping.debitCol != null && mapping.creditCol != null) {
            // Separate debit/credit columns
            rawDebit = safeGet(csv, mapping.debitCol);
            rawCredit = safeGet(csv, mapping.creditCol);
            BigDecimal debit = parseAmount(rawDebit);
            BigDecimal credit = parseAmount(rawCredit);

            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                amount = debit;
                direction = TransactionDirection.DEBIT;
            } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                amount = credit;
                direction = TransactionDirection.CREDIT;
            } else {
                throw new IllegalArgumentException(
                        "Row " + rowNumber + ": both debit and credit are empty or zero.");
            }
        } else {
            // Single amount column — direction determined by sign or separate indicator
            String rawAmount = safeGet(csv, mapping.amountCol);
            BigDecimal parsed = parseAmount(rawAmount);
            if (parsed == null) {
                throw new IllegalArgumentException(
                        "Row " + rowNumber + ": amount is missing or invalid: '" + rawAmount + "'");
            }
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                amount = parsed.negate();
                direction = TransactionDirection.DEBIT;
            } else {
                amount = parsed;
                direction = TransactionDirection.CREDIT;
            }
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Row " + rowNumber + ": transaction amount must be greater than zero.");
        }

        return new RawTransactionRecord(
                rowNumber, transactionDate, postedDate,
                description, amount, direction,
                rawDebit, rawCredit, rawDate
        );
    }

    // -------------------------------------------------------------------------
    // Column resolution
    // -------------------------------------------------------------------------

    private ColumnMapping resolveColumns(Map<String, Integer> headerMap) {
        ColumnMapping m = new ColumnMapping();
        for (String header : headerMap.keySet()) {
            String lower = header.toLowerCase().trim();
            String canonical = HEADER_ALIASES.get(lower);
            if (canonical != null) {
                assignCanonical(m, canonical, header);
            }
        }

        // Fallback fuzzy matching if exact alias wasn't found
        if (m.dateCol == null || m.descriptionCol == null || !m.hasAmountColumns()) {
            for (String header : headerMap.keySet()) {
                String lower = header.toLowerCase().trim();
                if (m.dateCol == null && lower.contains("date")) {
                    m.dateCol = header;
                } else if (m.descriptionCol == null && (lower.contains("desc") || lower.contains("narration") || lower.contains("particular") || lower.contains("remark"))) {
                    m.descriptionCol = header;
                } else if (m.debitCol == null && (lower.contains("debit") || lower.contains("withdraw") || lower.startsWith("dr"))) {
                    m.debitCol = header;
                } else if (m.creditCol == null && (lower.contains("credit") || lower.contains("deposit") || lower.startsWith("cr"))) {
                    m.creditCol = header;
                } else if (m.amountCol == null && (lower.contains("amount") || lower.contains("amt"))) {
                    m.amountCol = header;
                }
            }
        }

        return m;
    }

    private void assignCanonical(ColumnMapping m, String canonical, String header) {
        switch (canonical) {
            case "DATE"        -> { if (m.dateCol == null) m.dateCol = header; }
            case "POSTED_DATE" -> { if (m.postedDateCol == null) m.postedDateCol = header; }
            case "DESCRIPTION" -> { if (m.descriptionCol == null) m.descriptionCol = header; }
            case "DEBIT"       -> { if (m.debitCol == null) m.debitCol = header; }
            case "CREDIT"      -> { if (m.creditCol == null) m.creditCol = header; }
            case "AMOUNT"      -> { if (m.amountCol == null) m.amountCol = header; }
        }
    }

    private char detectDelimiter(String content) {
        if (content.contains("~|~")) return '|'; // apache csv splits ~|~ when delimiter is '|' or handle replacing
        if (content.contains("\t")) return '\t';
        if (content.contains(";")) return ';';
        if (content.contains("|")) return '|';
        return ',';
    }

    private int findHeaderLineIndex(String[] lines, char delimiter) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("date") && (line.contains("desc") || line.contains("narration") || line.contains("particular") || line.contains("summary")) &&
                    (line.contains("amount") || line.contains("amt") || line.contains("debit") || line.contains("credit") || line.contains("dr") || line.contains("cr"))) {
                return i;
            }
        }
        // Secondary attempt: any line with date and (amount or debit or credit)
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("date") && (line.contains("amount") || line.contains("amt") || line.contains("debit") || line.contains("credit"))) {
                return i;
            }
        }
        return -1;
    }

    private static class ColumnMapping {
        String dateCol;
        String postedDateCol;
        String descriptionCol;
        String debitCol;
        String creditCol;
        String amountCol;

        boolean hasAmountColumns() {
            return (debitCol != null && creditCol != null) || amountCol != null;
        }
    }

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("MM/dd/yy")
    );

    private LocalDate parseDate(String raw, int rowNumber) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Row " + rowNumber + ": date is missing.");
        }
        String cleaned = raw.replaceAll("~", "").trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Try parsing first token if timestamp space separated
        if (cleaned.contains(" ")) {
            String datePart = cleaned.split("\\s+")[0];
            for (DateTimeFormatter fmt : DATE_FORMATS) {
                try {
                    return LocalDate.parse(datePart, fmt);
                } catch (DateTimeParseException ignored) {}
            }
        }
        throw new IllegalArgumentException(
                "Row " + rowNumber + ": unrecognised date format '" + cleaned + "'.");
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Remove currency symbols, commas, spaces, tildes
        String cleaned = raw.replaceAll("~", "").trim()
                .replaceAll("[₹$€£,\\s]", "")
                .replaceAll("\\((.+)\\)", "-$1"); // (1000) → -1000
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            String val = record.get(column);
            if (val == null) return null;
            val = val.replaceAll("~", "").trim();
            return val;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
