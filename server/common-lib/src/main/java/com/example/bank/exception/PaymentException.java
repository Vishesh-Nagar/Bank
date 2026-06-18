package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public class PaymentException extends RuntimeException {

    private final ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
