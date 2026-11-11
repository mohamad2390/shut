package com.shut.telegram.project;

/** What the user typed or pressed cannot be used at this step. The message is shown to them as is. */
class InvalidProjectInputException extends RuntimeException {

    InvalidProjectInputException(String message) {
        super(message);
    }
}
