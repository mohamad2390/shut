package com.shut.telegram.rental;

/** What the member typed for a rental shop is not valid; the message says why, in Persian. */
class InvalidRentalInputException extends RuntimeException {

    InvalidRentalInputException(String message) {
        super(message);
    }
}
