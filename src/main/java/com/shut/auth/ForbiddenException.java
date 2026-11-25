package com.shut.auth;

/** The member is signed in but may not do this, for instance a calendar needs a specialist profile. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
