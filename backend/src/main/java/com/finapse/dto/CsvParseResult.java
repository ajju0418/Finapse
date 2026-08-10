package com.finapse.dto;

import java.util.List;

/**
 * Result of parsing a CSV file.
 * Contains successfully parsed records and a list of invalid row reports.
 * Invalid rows are never silently discarded.
 */
public record CsvParseResult(
        List<RawTransactionRecord> records,
        List<InvalidRowReport> invalidRows
) {
    public record InvalidRowReport(int rowNumber, String reason, String rawContent) {}
}
