package com.qalytix.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends QalytixException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
