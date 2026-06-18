package com.example.bank.exception;

import com.example.bank.common.ApiResponse;
import com.example.bank.enums.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String requestId(HttpServletRequest req) {
        String id = req.getHeader("X-Request-Id");
        return id != null ? id : "unknown";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest req) {
        List<ApiResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldError(
                        fe.getField(),
                        fe.getCode(),
                        fe.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("requestId={} path={} VALIDATION_FAILED", requestId(req), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.validationError(requestId(req), details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleMalformed(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        log.warn("requestId={} path={} MALFORMED_REQUEST", requestId(req), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("MALFORMED_REQUEST",
                        "The request body is malformed or unreadable.", requestId(req)));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<?>> handleUser(UserException ex, HttpServletRequest req) {
        HttpStatus status = ex.getErrorCode().getHttpStatus();
        log.warn("requestId={} path={} {} {}", requestId(req), req.getRequestURI(),
                ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getErrorCode().name(), ex.getMessage(), requestId(req)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest req) {
        log.warn("requestId={} path={} ACCESS_DENIED", requestId(req), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.name(),
                        "You do not have permission to perform this action.", requestId(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("requestId={} path={} INTERNAL_ERROR", requestId(req), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred. Please try again later.", requestId(req)));
    }
}
