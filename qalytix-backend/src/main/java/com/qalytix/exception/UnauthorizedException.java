package com.qalytix.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends QalytixException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
