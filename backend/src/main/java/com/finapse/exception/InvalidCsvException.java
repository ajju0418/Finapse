package com.finapse.exception;

public class InvalidCsvException extends RuntimeException {
    public InvalidCsvException(String message) {
        super(message);
    }
}
