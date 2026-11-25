package com.shut.auth;

/** The code is wrong, expired or already used. One message for all three, so nobody can tell which. */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("The code is wrong or has expired");
    }
}
