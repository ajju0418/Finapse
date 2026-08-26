package com.finapse.service;

import com.finapse.dto.RawTransactionRecord;
import com.finapse.dto.StatementParseResult;
import com.finapse.dto.StatementParseResult.InvalidRowReport;
import com.finapse.enums.TransactionDirection;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HdfcPdfStatementParser implements StatementFileParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    // Pattern to match the start of a transaction row
    // Format: "dd/MM/yy Narration..."
    private static final Pattern ROW_START_PATTERN = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2})\\s+(.*)");

    // Pattern to match the end of a transaction row
    // Format: "... [Chq./Ref.No.] ValueDt Amount Balance"
    // Note: Chq No can be empty, so we look for ValueDt, Amount, and Balance at the end.
    private static final Pattern ROW_END_PATTERN = Pattern.compile("(.*?)(\\d{2}/\\d{2}/\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})$");

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public StatementParseResult parse(InputStream inputStream, String fileName) {
        List<RawTransactionRecord> records = new ArrayList<>();
        List<InvalidRowReport> invalidRows = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
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
                    currentDateStr = startMatcher.group(1);
                    currentRestOfLine = startMatcher.group(2);
                    currentNarration = new StringBuilder();
                } else if (currentDateStr != null) {
                    // It's a continuation of the narration for the current record
                    if (!currentNarration.isEmpty()) {
                        currentNarration.append(" ");
                    }
                    currentNarration.append(line);
                }
            }

            // Flush the last record
            if (currentDateStr != null) {
                processRecord(currentTransactionRowNum, currentDateStr, currentNarration.toString(), currentRestOfLine, records, invalidRows);
            }

            // Post-process to determine TransactionDirection using Balance Delta
            determineDirections(records);

        } catch (Exception e) {
            log.error("Failed to parse PDF statement {}", fileName, e);
            throw new RuntimeException("Error parsing PDF statement: " + e.getMessage());
        }

        return new StatementParseResult(records, invalidRows);
    }

    private void processRecord(int rowNum, String dateStr, String appendedNarration, String restOfFirstLine,
                               List<RawTransactionRecord> records, List<InvalidRowReport> invalidRows) {
        try {
            Matcher endMatcher = ROW_END_PATTERN.matcher(restOfFirstLine);
            if (!endMatcher.find()) {
                invalidRows.add(new InvalidRowReport(rowNum, "Could not match amounts and balance at the end of the line", restOfFirstLine));
                return;
            }

            String narrationPart = endMatcher.group(1).trim();
            String valueDateStr = endMatcher.group(2).trim();
            String amountStr = endMatcher.group(3).trim();
            String balanceStr = endMatcher.group(4).trim();

            // Combine narration
            String fullNarration = narrationPart;
            if (!appendedNarration.isEmpty()) {
                fullNarration = fullNarration + " " + appendedNarration;
            }

            LocalDate transactionDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            LocalDate postedDate = LocalDate.parse(valueDateStr, DATE_FORMATTER);
            BigDecimal amount = parseAmount(amountStr);
            BigDecimal balance = parseAmount(balanceStr);

            // We temporarily set direction to null, we will deduce it in post-processing
            // We'll store the balance in the rawCredit field temporarily just to pass it to post-processing
            records.add(new RawTransactionRecord(
                    rowNum,
                    transactionDate,
                    postedDate,
                    fullNarration,
                    amount,
                    null, // Direction unknown yet
                    amountStr, // rawDebit
                    balanceStr, // rawCredit (hijacking to store balance for delta calc)
                    dateStr
            ));

        } catch (DateTimeParseException e) {
            invalidRows.add(new InvalidRowReport(rowNum, "Invalid date format", dateStr));
        } catch (Exception e) {
            invalidRows.add(new InvalidRowReport(rowNum, "Failed to parse record: " + e.getMessage(), restOfFirstLine));
        }
    }

    private void determineDirections(List<RawTransactionRecord> records) {
        List<RawTransactionRecord> updatedRecords = new ArrayList<>();
        
        for (int i = 0; i < records.size(); i++) {
            RawTransactionRecord current = records.get(i);
            BigDecimal amount = current.amount();
            BigDecimal currentBalance = parseAmount(current.rawCredit()); // We temporarily stored balance here

            TransactionDirection direction = TransactionDirection.DEBIT; // Default

            if (i > 0) {
                // Compare with previous row's balance from the ORIGINAL records list
                RawTransactionRecord prev = records.get(i - 1);
                BigDecimal prevBalance = parseAmount(prev.rawCredit());

                if (prevBalance.subtract(amount).compareTo(currentBalance) == 0) {
                    direction = TransactionDirection.DEBIT;
                } else if (prevBalance.add(amount).compareTo(currentBalance) == 0) {
                    direction = TransactionDirection.CREDIT;
                }
            } else {
                // For the very first row, we fallback to heuristics
                if (records.size() > 1) {
                    String desc = current.description().toUpperCase();
                    if (desc.contains("CASHBACK") || desc.contains("CREDIT") || desc.contains("SALARY") || desc.contains("REVERSAL")) {
                        direction = TransactionDirection.CREDIT;
                    }
                }
            }

            // Create a new record with the correct direction and clear the hijacked fields
            String rawDebit = direction == TransactionDirection.DEBIT ? amount.toString() : "";
            String rawCredit = direction == TransactionDirection.CREDIT ? amount.toString() : "";

            updatedRecords.add(new RawTransactionRecord(
                    current.sourceRowNumber(),
                    current.transactionDate(),
                    current.postedDate(),
                    current.description(),
                    amount,
                    direction,
                    rawDebit,
                    rawCredit,
                    current.rawDate()
            ));
        }
        
        records.clear();
        records.addAll(updatedRecords);
    }

    private BigDecimal parseAmount(String amountStr) {
        return new BigDecimal(amountStr.replace(",", ""));
    }
}
