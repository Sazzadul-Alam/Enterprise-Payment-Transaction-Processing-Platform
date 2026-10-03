package com.enterprise.payment.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BaseException {

    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public BusinessValidationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
