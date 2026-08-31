package com.finapse.exception;

/** Credentials are missing, malformed, expired or simply wrong. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
