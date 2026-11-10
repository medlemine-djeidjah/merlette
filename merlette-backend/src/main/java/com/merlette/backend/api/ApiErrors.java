package com.merlette.backend.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    public static Map<String, Object> body(String code, String message) {
        return Map.of("error", Map.of("code", code, "message", message, "details", List.of()));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> application(ApiException e) {
        return ResponseEntity.status(e.status).body(body(e.code, e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var details = e.getBindingResult().getFieldErrors().stream()
            .map(f -> Map.of("field", f.getField(), "message", Objects.requireNonNullElse(f.getDefaultMessage(), "Valeur invalide")))
            .toList();
        return ResponseEntity.badRequest().body(Map.of("error", Map.of(
            "code", "VALIDATION_ERROR", "message", "Les données envoyées sont invalides.", "details", details)));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        ServletRequestBindingException.class})
    ResponseEntity<?> malformed(Exception e) {
        return ResponseEntity.badRequest().body(body("VALIDATION_ERROR", "Requête invalide ou champ obligatoire manquant."));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() {
        return ResponseEntity.status(413).body(body("FILE_TOO_LARGE", "Chaque photo doit faire au maximum 5 Mo."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(body("CONFLICT", "Ces données existent déjà ou ne respectent pas les contraintes."));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<?> missingResource() {
        return ResponseEntity.status(404).body(body("NOT_FOUND", "Ressource introuvable."));
    }
    @ExceptionHandler({org.springframework.web.HttpRequestMethodNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotSupportedException.class})
    ResponseEntity<?> unsupported(org.springframework.web.ErrorResponse e) {
        return ResponseEntity.status(e.getStatusCode()).body(body("INVALID_REQUEST", "Méthode HTTP ou type de contenu non pris en charge."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Erreur serveur inattendue", e);
        return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR", "Une erreur serveur est survenue."));
    }
}
