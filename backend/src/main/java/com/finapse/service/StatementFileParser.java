package com.finapse.service;

import com.finapse.dto.StatementParseResult;
import java.io.InputStream;

public interface StatementFileParser {
    boolean supports(String fileName);
    StatementParseResult parse(InputStream inputStream, String fileName);
}
