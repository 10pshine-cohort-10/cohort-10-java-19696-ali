package com.shine.book;

import java.util.Map;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;

@RestControllerAdvice
class ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    private ResponseEntity<Map<String,String>> error(HttpStatusCode status, String message) {
        log.warn("Request rejected, HTTP {}", status.value());
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
    @ExceptionHandler(ApiError.class)
    ResponseEntity<Map<String,String>> known(ApiError ex) { return error(ex.status, ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldErrors().stream().findFirst();
        return error(HttpStatus.BAD_REQUEST, field.map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("Check your entries."));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String,String>> invalid() { return error(HttpStatus.BAD_REQUEST, "Check the request format."); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,String>> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse web) return error(web.getStatusCode(), "This request could not be processed.");
        log.error("Unexpected application error", ex);
        return ResponseEntity.internalServerError().body(Map.of("message", "Something went wrong. Try again."));
    }
}
