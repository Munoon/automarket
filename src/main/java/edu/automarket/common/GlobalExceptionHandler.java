package edu.automarket.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDTO> handleApiException(ApiException ex,
                                                         @AuthenticationPrincipal Long userId) {
        if (userId != null) {
            log.error("Handled API exception for user {}", userId, ex);
        } else {
            log.error("Handled API exception for annonymous user", ex);
        }
        return ResponseEntity
                .status(ex.getStatus())
                .contentType(PROBLEM_JSON)
                .body(ex.toProblem());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDTO> handleValidationException(WebExchangeBindException ex,
                                                                @AuthenticationPrincipal Long userId) {
        if (userId != null) {
            log.error("Handled validation exception for user {}", userId, ex);
        } else {
            log.error("Handled validation exception for annonymous user", ex);
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(new ProblemDTO("/problems/validation-error", "Validation Error", 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDTO> handleException(Exception ex, @AuthenticationPrincipal Long userId) {
        if (userId != null) {
            log.error("Handled unknown exception for user {}", userId, ex);
        } else {
            log.error("Handled unknown exception for annonymous user", ex);
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(PROBLEM_JSON)
                .body(new ProblemDTO("/problems/internal-error", "Internal Server Error", 500));
    }
}
