package com.finapse.dto;

import java.util.List;

/**
 * Result of a dry-run parse. Nothing is persisted — the client uses this to
 * confirm the detected columns, remap them if needed, and see a sample of the
 * rows before committing the import.
 */
public record StatementPreviewResponse(
        String fileName,
        /** Every header found in the file, so the UI can offer them for remapping. */
        List<String> availableColumns,
        ColumnMappingOverride detectedMapping,
        boolean mappingComplete,
        String message,
        int parsedRowCount,
        int invalidRowCount,
        List<PreviewRow> sampleRows,
        List<StatementParseResult.InvalidRowReport> sampleInvalidRows,
        boolean passwordRequired
) {
    public StatementPreviewResponse(
            String fileName,
            List<String> availableColumns,
            ColumnMappingOverride detectedMapping,
            boolean mappingComplete,
            String message,
            int parsedRowCount,
            int invalidRowCount,
            List<PreviewRow> sampleRows,
            List<StatementParseResult.InvalidRowReport> sampleInvalidRows
    ) {
        this(fileName, availableColumns, detectedMapping, mappingComplete, message,
                parsedRowCount, invalidRowCount, sampleRows, sampleInvalidRows, false);
    }

    public record PreviewRow(
            int sourceRowNumber,
            String date,
            String description,
            String amount,
            String direction
    ) {}
}
