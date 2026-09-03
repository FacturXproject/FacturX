package com.facturx.app.organization;

public class NoAccountFoundException extends RuntimeException {
    public NoAccountFoundException(String message) {
        super(message);
    }
}