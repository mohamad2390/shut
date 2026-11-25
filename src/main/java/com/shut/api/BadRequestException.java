package com.shut.api;

/** The client asked for something that cannot be answered, such as a date range that is too long. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
