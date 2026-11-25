package com.shut.telegram.studio;

/** What the member typed for a studio detail is not valid; the message says why, in Persian. */
class InvalidStudioInputException extends RuntimeException {

    InvalidStudioInputException(String message) {
        super(message);
    }
}
