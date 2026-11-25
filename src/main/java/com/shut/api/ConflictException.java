package com.shut.api;

/** The request is fine but cannot be done now, for instance a list is already full. {@code code} tells which. */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
