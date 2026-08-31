package com.finapse.dto;

/**
 * User-supplied header names that override Finapse's automatic column detection.
 * Any null field falls back to the detected column.
 */
public record ColumnMappingOverride(
        String dateColumn,
        String postedDateColumn,
        String descriptionColumn,
        String debitColumn,
        String creditColumn,
        String amountColumn
) {
    public static final ColumnMappingOverride NONE =
            new ColumnMappingOverride(null, null, null, null, null, null);

    public boolean isEmpty() {
        return blank(dateColumn) && blank(postedDateColumn) && blank(descriptionColumn)
                && blank(debitColumn) && blank(creditColumn) && blank(amountColumn);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
