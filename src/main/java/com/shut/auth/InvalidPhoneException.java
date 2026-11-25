package com.shut.auth;

/** The text is not an Iranian mobile number. */
public class InvalidPhoneException extends RuntimeException {

    public InvalidPhoneException() {
        super("Enter an Iranian mobile number, e.g. 09121234567");
    }
}
