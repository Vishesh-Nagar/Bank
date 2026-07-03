package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public abstract class BaseAppException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseAppException(ErrorCode errorCode) {
        super(ErrorMessageConstants.getMessage(errorCode));
        this.errorCode = errorCode;
    }

    public BaseAppException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public BaseAppException(ErrorCode errorCode, Throwable cause) {
        super(ErrorMessageConstants.getMessage(errorCode), cause);
        this.errorCode = errorCode;
    }

    public BaseAppException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
