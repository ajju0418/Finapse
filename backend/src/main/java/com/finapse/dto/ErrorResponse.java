package com.finapse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ErrorResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        int status,
        String code,
        String message
) {
    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, code, message);
    }
}
