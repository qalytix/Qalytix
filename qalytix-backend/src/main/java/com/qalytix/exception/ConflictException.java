package com.qalytix.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends QalytixException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
