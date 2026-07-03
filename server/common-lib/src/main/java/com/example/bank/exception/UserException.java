package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public class UserException extends BaseAppException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
