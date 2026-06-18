package com.example.bank.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Authentication & Authorization
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    ACCOUNT_OWNERSHIP_REQUIRED(HttpStatus.FORBIDDEN),

    // Validation
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_MISSING(HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT),

    // Resource
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),

    // Business Logic
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY),
    SELF_TRANSFER_NOT_ALLOWED(HttpStatus.CONFLICT),
    NEGATIVE_AMOUNT(HttpStatus.UNPROCESSABLE_ENTITY),
    ACCOUNT_HAS_BALANCE(HttpStatus.CONFLICT),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT),

    // Rate Limiting
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),

    // Server Errors
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    DOWNSTREAM_SERVICE_ERROR(HttpStatus.BAD_GATEWAY);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
