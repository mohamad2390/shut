package com.shut.api.media;

/** The media exists in our data but its bytes cannot be fetched right now. */
public class MediaUnavailableException extends RuntimeException {

    public MediaUnavailableException(String message) {
        super(message);
    }
}
