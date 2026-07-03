package com.example.bank.exception;

import com.example.bank.enums.ErrorCode;

public class PaymentException extends BaseAppException {

    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
