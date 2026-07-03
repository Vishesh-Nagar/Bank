package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public class AccountException extends BaseAppException {

    public AccountException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AccountException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
