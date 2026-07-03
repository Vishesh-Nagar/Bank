package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;
import java.util.EnumMap;
import java.util.Map;

public class ErrorMessageConstants {

    private static final Map<ErrorCode, String> MESSAGES = new EnumMap<>(ErrorCode.class);

    static {
        // Authentication & Authorization
        MESSAGES.put(ErrorCode.AUTHENTICATION_REQUIRED, "You must be logged in to access this resource.");
        MESSAGES.put(ErrorCode.INVALID_TOKEN, "Your session token is invalid. Please log in again.");
        MESSAGES.put(ErrorCode.TOKEN_EXPIRED, "Your session has expired. Please log in again.");
        MESSAGES.put(ErrorCode.INVALID_CREDENTIALS, "The username or password you entered is incorrect.");
        MESSAGES.put(ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action.");
        MESSAGES.put(ErrorCode.ACCOUNT_OWNERSHIP_REQUIRED, "You can only access or modify your own accounts.");
        MESSAGES.put(ErrorCode.EMAIL_NOT_VERIFIED, "Please verify your email address before continuing.");

        // Validation
        MESSAGES.put(ErrorCode.VALIDATION_FAILED, "Please check the highlighted fields and try again.");
        MESSAGES.put(ErrorCode.MALFORMED_REQUEST, "The request was malformed or could not be processed.");
        MESSAGES.put(ErrorCode.IDEMPOTENCY_KEY_MISSING, "An idempotency key is required for this operation.");
        MESSAGES.put(ErrorCode.IDEMPOTENCY_KEY_REUSED, "This request has already been processed.");

        // Resource
        MESSAGES.put(ErrorCode.USER_NOT_FOUND, "The requested user could not be found.");
        MESSAGES.put(ErrorCode.ACCOUNT_NOT_FOUND, "The requested account could not be found.");
        MESSAGES.put(ErrorCode.PAYMENT_NOT_FOUND, "The specified payment record could not be found.");
        MESSAGES.put(ErrorCode.USERNAME_ALREADY_EXISTS, "This username is already taken. Please choose another.");
        MESSAGES.put(ErrorCode.EMAIL_ALREADY_EXISTS, "An account with this email address already exists.");

        // Business Logic
        MESSAGES.put(ErrorCode.INSUFFICIENT_BALANCE, "You do not have sufficient balance for this transaction.");
        MESSAGES.put(ErrorCode.SELF_TRANSFER_NOT_ALLOWED, "Transfers to the same account are not permitted.");
        MESSAGES.put(ErrorCode.NEGATIVE_AMOUNT, "The transaction amount must be greater than zero.");
        MESSAGES.put(ErrorCode.ACCOUNT_HAS_BALANCE, "Cannot close an account with a non-zero balance.");
        MESSAGES.put(ErrorCode.PAYMENT_ALREADY_PROCESSED, "This payment has already been processed.");
        MESSAGES.put(ErrorCode.DAILY_LIMIT_EXCEEDED, "This transaction exceeds your daily transfer limit.");

        // Rate Limiting
        MESSAGES.put(ErrorCode.RATE_LIMITED, "You are making too many requests. Please slow down and try again later.");

        // Server Errors
        MESSAGES.put(ErrorCode.INTERNAL_ERROR, "An unexpected internal error occurred. Please try again later.");
        MESSAGES.put(ErrorCode.SERVICE_UNAVAILABLE, "The service is temporarily unavailable. Please try again later.");
        MESSAGES.put(ErrorCode.DOWNSTREAM_SERVICE_ERROR, "We are experiencing issues connecting to a downstream service.");
    }

    public static String getMessage(ErrorCode code) {
        return MESSAGES.getOrDefault(code, "An unexpected error occurred.");
    }
}
