package com.shut.auth;

/** The request has no valid sign-in. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
