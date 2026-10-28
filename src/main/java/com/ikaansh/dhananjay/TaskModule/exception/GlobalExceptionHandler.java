package com.ikaansh.dhananjay.TaskModule.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One error format for the whole API. Extends ResponseEntityExceptionHandler so every
 * standard Spring MVC error (405, 415, 404, missing params, ...) also uses ApiErrorResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------------- 400 ----------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String message = details.stream()
                .map(d -> d.getField() + ": " + d.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Validation failed" : message, request, details));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(build(HttpStatus.BAD_REQUEST,
                "Malformed or unreadable request body (check JSON syntax, enum values and date format yyyy-MM-dd)",
                request, null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (status.is5xxServerError()) {
            log.error("Server error", ex);
        }
        String message = status.is5xxServerError() ? "An unexpected error occurred" : ex.getMessage();
        return ResponseEntity.status(status).headers(headers).body(build(status, message, request, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    return path.substring(path.lastIndexOf('.') + 1) + ": " + v.getMessage();
                })
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler({InvalidTaskException.class, AuthenticationException.class, InvalidTokenException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ---------------- 401 ----------------

    @ExceptionHandler({InvalidCredentialsException.class, JwtValidationException.class, OAuth2AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException ex, WebRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleSpringAuthentication(
            org.springframework.security.core.AuthenticationException ex, WebRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "Authentication required", request);
    }

    // ---------------- 403 ----------------

    @ExceptionHandler({ForbiddenOperationException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(RuntimeException ex, WebRequest request) {
        String message = ex instanceof ForbiddenOperationException
                ? ex.getMessage()
                : "You do not have permission to access this resource";
        return respond(HttpStatus.FORBIDDEN, message, request);
    }

    // ---------------- 404 ----------------

    @ExceptionHandler({TaskNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ---------------- 409 ----------------

    @ExceptionHandler({DuplicateRequestException.class, UserAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException ex, WebRequest request) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Lock timeout, deadlock, optimistic version mismatch. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrency(ConcurrencyFailureException ex, WebRequest request) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        return respond(HttpStatus.CONFLICT,
                "The resource is being modified by another request. Please retry.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "Request conflicts with existing data", request);
    }

    // ---------------- 500 ----------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    // ---------------- helpers ----------------

    private ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(build(status, message, request, null));
    }

    private ApiErrorResponse build(HttpStatus status, String message, WebRequest request,
                                   List<ApiErrorResponse.FieldErrorDetail> errors) {
        String path = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
    }
}
