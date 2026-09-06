package com.finapse.service;

import com.finapse.dto.ColumnMappingOverride;
import com.finapse.dto.StatementParseResult;

import java.io.InputStream;
import java.util.List;

public interface StatementFileParser {

    boolean supports(String fileName);

    StatementParseResult parse(InputStream inputStream, String fileName);

    /**
     * Parses with user-supplied column names taking precedence over automatic
     * detection. Parsers without a header row ignore the override.
     */
    default StatementParseResult parse(InputStream inputStream, String fileName,
                                       ColumnMappingOverride override) {
        return parse(inputStream, fileName);
    }

    /**
     * Parses with user-supplied column names and optional password for encrypted files.
     */
    default StatementParseResult parse(InputStream inputStream, String fileName,
                                       ColumnMappingOverride override, String password) {
        return parse(inputStream, fileName, override);
    }

    /**
     * Header names present in the file, used to build the column-mapping UI.
     * Empty for formats that have no header row.
     */
    default List<String> readColumnNames(InputStream inputStream, String fileName) {
        return List.of();
    }

    default List<String> readColumnNames(InputStream inputStream, String fileName, String password) {
        return readColumnNames(inputStream, fileName);
    }

    /** Columns this parser detected automatically, for confirmation in the UI. */
    default ColumnMappingOverride detectMapping(InputStream inputStream, String fileName) {
        return ColumnMappingOverride.NONE;
    }

    default ColumnMappingOverride detectMapping(InputStream inputStream, String fileName, String password) {
        return detectMapping(inputStream, fileName);
    }
}
