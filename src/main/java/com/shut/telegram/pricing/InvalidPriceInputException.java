package com.shut.telegram.pricing;

/** The user's text cannot be used as a price. The message is shown to them as is. */
class InvalidPriceInputException extends RuntimeException {

    InvalidPriceInputException(String message) {
        super(message);
    }
}
