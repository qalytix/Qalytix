package com.qalytix.exception;

import org.springframework.http.HttpStatus;

public class QalytixException extends RuntimeException {

    private final HttpStatus status;

    public QalytixException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
