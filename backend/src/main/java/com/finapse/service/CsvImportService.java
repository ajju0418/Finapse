package com.finapse.service;

import com.finapse.dto.CsvParseResult;
import com.finapse.dto.CsvParseResult.InvalidRowReport;
import com.finapse.dto.RawTransactionRecord;
import com.finapse.enums.TransactionDirection;
import com.finapse.exception.InvalidCsvException;
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
public class CsvImportService {

    // Supported date formats across common Indian bank/card statements
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yy")
    );

    // Canonical column name mappings — maps source header variants to internal names
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("date",               "DATE"),
            Map.entry("transaction date",   "DATE"),
            Map.entry("txn date",           "DATE"),
            Map.entry("value date",         "DATE"),
            Map.entry("posting date",       "POSTED_DATE"),
            Map.entry("posted date",        "POSTED_DATE"),
            Map.entry("description",        "DESCRIPTION"),
            Map.entry("narration",          "DESCRIPTION"),
            Map.entry("particulars",        "DESCRIPTION"),
            Map.entry("transaction details","DESCRIPTION"),
            Map.entry("remarks",            "DESCRIPTION"),
            Map.entry("debit",              "DEBIT"),
            Map.entry("debit amount",       "DEBIT"),
            Map.entry("withdrawal",         "DEBIT"),
            Map.entry("withdrawal (dr)",    "DEBIT"),
            Map.entry("dr",                 "DEBIT"),
            Map.entry("credit",             "CREDIT"),
            Map.entry("credit amount",      "CREDIT"),
            Map.entry("deposit",            "CREDIT"),
            Map.entry("deposit (cr)",       "CREDIT"),
            Map.entry("cr",                 "CREDIT"),
            Map.entry("amount",             "AMOUNT"),
            Map.entry("transaction amount", "AMOUNT")
    );

    public CsvParseResult parse(InputStream inputStream, String fileName) {
        List<RawTransactionRecord> records = new ArrayList<>();
        List<InvalidRowReport> invalidRows = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new InvalidCsvException(
                        "The uploaded CSV does not contain a recognizable header row.");
            }

            // Resolve canonical column names from the actual headers
            ColumnMapping mapping = resolveColumns(headerMap);
            if (mapping.dateCol == null) {
                throw new InvalidCsvException(
                        "Could not find a date column. Expected one of: Date, Transaction Date, Txn Date.");
            }
            if (mapping.descriptionCol == null) {
                throw new InvalidCsvException(
                        "Could not find a description column. Expected one of: Description, Narration, Particulars.");
            }
            if (!mapping.hasAmountColumns()) {
                throw new InvalidCsvException(
                        "Could not find amount columns. Expected Debit/Credit or Amount columns.");
            }

            // Row number starts at 2 (1 = header)
            int rowNumber = 2;
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

        } catch (InvalidCsvException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidCsvException("Failed to read CSV file: " + fileName);
        }

        if (records.isEmpty() && invalidRows.isEmpty()) {
            throw new InvalidCsvException("The CSV file contains no transaction rows.");
        }

        return new CsvParseResult(records, invalidRows);
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
            String canonical = HEADER_ALIASES.get(header.toLowerCase().trim());
            if (canonical == null) continue;
            switch (canonical) {
                case "DATE"        -> m.dateCol        = header;
                case "POSTED_DATE" -> m.postedDateCol  = header;
                case "DESCRIPTION" -> m.descriptionCol = header;
                case "DEBIT"       -> m.debitCol       = header;
                case "CREDIT"      -> m.creditCol      = header;
                case "AMOUNT"      -> { if (m.amountCol == null) m.amountCol = header; }
            }
        }
        return m;
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

    private LocalDate parseDate(String raw, int rowNumber) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Row " + rowNumber + ": date is missing.");
        }
        String cleaned = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException(
                "Row " + rowNumber + ": unrecognised date format '" + cleaned + "'.");
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Remove currency symbols, commas, spaces
        String cleaned = raw.trim()
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
            return val != null ? val.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
