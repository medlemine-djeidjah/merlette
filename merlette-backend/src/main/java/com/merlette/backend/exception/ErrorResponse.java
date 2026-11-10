package com.merlette.backend.exception;

import java.util.List;

/** Format d'erreur uniforme : { "error": { "code", "message", "details" } }. */
public record ErrorResponse(Body error) {

    public record Body(String code, String message, List<FieldError> details) {}

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Body(code, message, null));
    }

    public static ErrorResponse of(String code, String message, List<FieldError> details) {
        return new ErrorResponse(new Body(code, message, details));
    }
}
