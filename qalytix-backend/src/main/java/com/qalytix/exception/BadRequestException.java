package com.qalytix.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends QalytixException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
