package com.finapse.exception;

import com.finapse.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateStatementException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateStatement(DuplicateStatementException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "DUPLICATE_STATEMENT", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCsv(InvalidCsvException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "INVALID_CSV", ex.getMessage()));
    }

    @ExceptionHandler(StatementProcessingException.class)
    public ResponseEntity<ErrorResponse> handleStatementProcessing(StatementProcessingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "STATEMENT_PROCESSING_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<ErrorResponse> handleCsvParse(CsvParseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "CSV_PARSE_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "VALIDATION_ERROR", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(413, "FILE_TOO_LARGE", "Uploaded file exceeds the maximum allowed size of 10MB."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred. Please try again."));
    }
}
