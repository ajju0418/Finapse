package com.finapse.service;

import com.finapse.dto.RawTransactionRecord;
import com.finapse.dto.StatementParseResult;
import com.finapse.dto.StatementParseResult.InvalidRowReport;
import com.finapse.enums.TransactionDirection;
import com.finapse.exception.InvalidStatementFileException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ExcelImportService implements StatementFileParser {

    @Override
    public boolean supports(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

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

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int headerRowIndex = findHeaderRowIndex(sheet);
            if (headerRowIndex == -1) {
                throw new InvalidStatementFileException("Could not find a header row with Date and Amount/Debit/Credit columns.");
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).trim();
                if (!header.isEmpty()) {
                    headerMap.put(header, cell.getColumnIndex());
                }
            }

            ColumnMapping mapping = resolveColumns(headerMap);
            if (mapping.dateCol == null) {
                throw new InvalidStatementFileException("Could not find a date column.");
            }
            if (mapping.descriptionCol == null) {
                throw new InvalidStatementFileException("Could not find a description column.");
            }
            if (!mapping.hasAmountColumns()) {
                throw new InvalidStatementFileException("Could not find amount columns.");
            }

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                int rowNumber = i + 1;
                String rawLine = "Row " + rowNumber; // Fallback representation for Excel
                try {
                    RawTransactionRecord record = parseRow(row, mapping, rowNumber, headerMap);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    invalidRows.add(new InvalidRowReport(rowNumber, e.getMessage(), rawLine));
                }
            }

        } catch (InvalidStatementFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidStatementFileException("Failed to read Excel file: " + fileName);
        }

        if (records.isEmpty() && invalidRows.isEmpty()) {
            throw new InvalidStatementFileException("The Excel file contains no transaction rows.");
        }

        return new StatementParseResult(records, invalidRows);
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 50); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            StringBuilder sb = new StringBuilder();
            for (Cell cell : row) {
                sb.append(getCellValueAsString(cell).toLowerCase()).append(" ");
            }
            String line = sb.toString();
            
            if (line.contains("date") && (line.contains("desc") || line.contains("narration") || line.contains("particular") || line.contains("summary")) &&
                    (line.contains("amount") || line.contains("amt") || line.contains("debit") || line.contains("credit") || line.contains("dr") || line.contains("cr"))) {
                return i;
            }
        }
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 50); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            StringBuilder sb = new StringBuilder();
            for (Cell cell : row) {
                sb.append(getCellValueAsString(cell).toLowerCase()).append(" ");
            }
            String line = sb.toString();
            if (line.contains("date") && (line.contains("amount") || line.contains("amt") || line.contains("debit") || line.contains("credit"))) {
                return i;
            }
        }
        return -1;
    }

    private RawTransactionRecord parseRow(Row row, ColumnMapping mapping, int rowNumber, Map<String, Integer> headerMap) {
        String rawDate = safeGet(row, mapping.dateCol, headerMap);
        String rawDesc = safeGet(row, mapping.descriptionCol, headerMap);

        if (rawDate == null || rawDate.isBlank() || rawDesc == null || rawDesc.isBlank()) {
            return null;
        }

        LocalDate transactionDate = parseDate(rawDate, rowNumber);
        LocalDate postedDate = null;
        if (mapping.postedDateCol != null) {
            String rawPosted = safeGet(row, mapping.postedDateCol, headerMap);
            if (rawPosted != null && !rawPosted.isBlank()) {
                try { postedDate = parseDate(rawPosted, rowNumber); } catch (Exception ignored) {}
            }
        }

        String description = rawDesc.trim();

        BigDecimal amount;
        TransactionDirection direction;
        String rawDebit = null;
        String rawCredit = null;

        if (mapping.debitCol != null && mapping.creditCol != null) {
            rawDebit = safeGet(row, mapping.debitCol, headerMap);
            rawCredit = safeGet(row, mapping.creditCol, headerMap);
            BigDecimal debit = parseAmount(rawDebit);
            BigDecimal credit = parseAmount(rawCredit);

            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                amount = debit;
                direction = TransactionDirection.DEBIT;
            } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                amount = credit;
                direction = TransactionDirection.CREDIT;
            } else {
                throw new IllegalArgumentException("Row " + rowNumber + ": both debit and credit are empty or zero.");
            }
        } else {
            String rawAmount = safeGet(row, mapping.amountCol, headerMap);
            BigDecimal parsed = parseAmount(rawAmount);
            if (parsed == null) {
                throw new IllegalArgumentException("Row " + rowNumber + ": amount is missing or invalid: '" + rawAmount + "'");
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
            throw new IllegalArgumentException("Row " + rowNumber + ": transaction amount must be greater than zero.");
        }

        return new RawTransactionRecord(
                rowNumber, transactionDate, postedDate,
                description, amount, direction,
                rawDebit, rawCredit, rawDate
        );
    }

    private String safeGet(Row row, String column, Map<String, Integer> headerMap) {
        if (column == null || !headerMap.containsKey(column)) return null;
        int colIdx = headerMap.get(column);
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        String val = getCellValueAsString(cell);
        if (val == null) return null;
        return val.replaceAll("~", "").trim();
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    // --- Reuse resolution and parsing logic ---

    private ColumnMapping resolveColumns(Map<String, Integer> headerMap) {
        ColumnMapping m = new ColumnMapping();
        for (String header : headerMap.keySet()) {
            String lower = header.toLowerCase().trim();
            String canonical = HEADER_ALIASES.get(lower);
            if (canonical != null) assignCanonical(m, canonical, header);
        }
        if (m.dateCol == null || m.descriptionCol == null || !m.hasAmountColumns()) {
            for (String header : headerMap.keySet()) {
                String lower = header.toLowerCase().trim();
                if (m.dateCol == null && lower.contains("date")) { m.dateCol = header; }
                else if (m.descriptionCol == null && (lower.contains("desc") || lower.contains("narration") || lower.contains("particular") || lower.contains("remark"))) { m.descriptionCol = header; }
                else if (m.debitCol == null && (lower.contains("debit") || lower.contains("withdraw") || lower.startsWith("dr"))) { m.debitCol = header; }
                else if (m.creditCol == null && (lower.contains("credit") || lower.contains("deposit") || lower.startsWith("cr"))) { m.creditCol = header; }
                else if (m.amountCol == null && (lower.contains("amount") || lower.contains("amt"))) { m.amountCol = header; }
            }
        }
        return m;
    }

    private void assignCanonical(ColumnMapping m, String canonical, String header) {
        switch (canonical) {
            case "DATE" -> { if (m.dateCol == null) m.dateCol = header; }
            case "POSTED_DATE" -> { if (m.postedDateCol == null) m.postedDateCol = header; }
            case "DESCRIPTION" -> { if (m.descriptionCol == null) m.descriptionCol = header; }
            case "DEBIT" -> { if (m.debitCol == null) m.debitCol = header; }
            case "CREDIT" -> { if (m.creditCol == null) m.creditCol = header; }
            case "AMOUNT" -> { if (m.amountCol == null) m.amountCol = header; }
        }
    }

    private static class ColumnMapping {
        String dateCol;
        String postedDateCol;
        String descriptionCol;
        String debitCol;
        String creditCol;
        String amountCol;
        boolean hasAmountColumns() { return (debitCol != null && creditCol != null) || amountCol != null; }
    }

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
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Row " + rowNumber + ": date is missing.");
        String cleaned = raw.replaceAll("~", "").trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(cleaned, fmt); } catch (DateTimeParseException ignored) {}
        }
        if (cleaned.contains(" ")) {
            String datePart = cleaned.split("\\s+")[0];
            for (DateTimeFormatter fmt : DATE_FORMATS) {
                try { return LocalDate.parse(datePart, fmt); } catch (DateTimeParseException ignored) {}
            }
        }
        throw new IllegalArgumentException("Row " + rowNumber + ": unrecognised date format '" + cleaned + "'.");
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("~", "").trim()
                .replaceAll("[₹$€£,\\s]", "")
                .replaceAll("\\((.+)\\)", "-$1");
        if (cleaned.isEmpty()) return null;
        try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return null; }
    }
}
