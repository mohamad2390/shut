package com.shut.api;

/** The thing the request names does not exist, or is not the member's. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
