package com.shut.telegram.profile;

/** The user's text cannot be used for the profile field. The message is shown to them as is. */
class InvalidProfileInputException extends RuntimeException {

    InvalidProfileInputException(String message) {
        super(message);
    }
}
