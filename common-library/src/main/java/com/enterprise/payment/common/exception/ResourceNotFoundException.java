package com.enterprise.payment.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s with ID '%s' was not found", resourceName, id), "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
