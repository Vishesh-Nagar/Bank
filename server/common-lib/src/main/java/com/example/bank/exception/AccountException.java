package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public class AccountException extends RuntimeException {

    private final ErrorCode errorCode;

    public AccountException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
