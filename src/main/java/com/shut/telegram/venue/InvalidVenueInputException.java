package com.shut.telegram.venue;

/** A venue detail the owner typed that cannot be used; the message says why, in Persian, for the owner. */
class InvalidVenueInputException extends RuntimeException {

    InvalidVenueInputException(String message) {
        super(message);
    }
}
