package com.shut.telegram.market;

/** What a seller typed for a listing is not valid; the message says why, in Persian. */
class InvalidListingInputException extends RuntimeException {

    InvalidListingInputException(String message) {
        super(message);
    }
}
