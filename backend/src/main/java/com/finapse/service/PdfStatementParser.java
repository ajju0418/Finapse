package com.finapse.service;

import com.finapse.dto.StatementParseResult;
import com.finapse.exception.InvalidStatementFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Entry point for every PDF statement. Tries each bank-specific layout in turn
 * and falls back to the generic reader, so an unrecognised bank still imports
 * instead of failing outright.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfStatementParser implements StatementFileParser {

    private final HdfcPdfStatementParser hdfcParser;
    private final HdfcCreditCardPdfStatementParser hdfcCreditCardParser;
    private final GenericPdfStatementParser genericParser;

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public StatementParseResult parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null, null);
    }

    @Override
    public StatementParseResult parse(InputStream inputStream, String fileName,
                                       com.finapse.dto.ColumnMappingOverride override, String password) {
        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new InvalidStatementFileException("Could not read the uploaded PDF: " + fileName);
        }

        StatementParseResult hdfc = hdfcParser.parse(new ByteArrayInputStream(content), fileName, password);
        if (!hdfc.records().isEmpty()) {
            log.info("PDF {} parsed with the HDFC layout: {} rows", fileName, hdfc.records().size());
            return hdfc;
        }

        StatementParseResult hdfcCc = hdfcCreditCardParser.parse(new ByteArrayInputStream(content), fileName, password);
        if (!hdfcCc.records().isEmpty()) {
            log.info("PDF {} parsed with the HDFC Credit Card layout: {} rows", fileName, hdfcCc.records().size());
            return hdfcCc;
        }

        StatementParseResult generic = genericParser.parse(new ByteArrayInputStream(content), fileName, password);
        if (!generic.records().isEmpty()) {
            log.info("PDF {} parsed with the generic layout: {} rows", fileName, generic.records().size());
            return generic;
        }

        throw new InvalidStatementFileException(
                "No transactions could be read from this PDF. It may be a scanned image rather than text, "
                + "or use a layout Finapse does not recognise yet. Try exporting the statement as CSV or Excel instead.");
    }
}
