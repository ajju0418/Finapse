package com.finapse.exception;

/** Generic client-side input error that is not tied to statement parsing. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
