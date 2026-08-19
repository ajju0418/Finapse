package com.finapse.service;

import com.finapse.exception.InvalidStatementFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementParserFactory {

    private final List<StatementFileParser> parsers;

    public StatementFileParser getParser(String fileName) {
        if (fileName == null) {
            throw new InvalidStatementFileException("File name cannot be null.");
        }
        return parsers.stream()
                .filter(parser -> parser.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new InvalidStatementFileException(
                        "Unsupported file type. Please upload a .csv, .xls, or .xlsx file."));
    }
}
