package com.shut.auth;

/** A code was asked for too soon after the last one, or too many times in an hour. */
public class TooManyOtpRequestsException extends RuntimeException {

    public TooManyOtpRequestsException(String message) {
        super(message);
    }
}
