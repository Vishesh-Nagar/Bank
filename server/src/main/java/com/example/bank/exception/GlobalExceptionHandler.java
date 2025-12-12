package com.example.bank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorDetails> buildError(Exception ex, WebRequest req, HttpStatus status, String code) {
        ErrorDetails error = new ErrorDetails(
                LocalDateTime.now(),
                ex.getMessage(),
                req.getDescription(false),
                code
        );
        return new ResponseEntity<>(error, status);
    }

    // Custom Exceptions
    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorDetails> handleAccountException(AccountException ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorDetails> handleUserException(UserException ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    // Bad JSON / Missing fields
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> handleInvalidJson(HttpMessageNotReadableException ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.BAD_REQUEST, "INVALID_JSON");
    }

    // Wrong type in path variable or query param
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetails> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.BAD_REQUEST, "TYPE_MISMATCH");
    }

    // Missing query params (if any)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDetails> handleMissingParam(MissingServletRequestParameterException ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.BAD_REQUEST, "MISSING_PARAMETER");
    }

    // Validation errors (if you use @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        return buildError(new Exception(msg), req, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    // Fallback generic exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGeneric(Exception ex, WebRequest req) {
        return buildError(ex, req, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }
}