package com.shut.api.media;

/** The picture or video does not exist, or does not belong to the page it was asked under. */
public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException(String message) {
        super(message);
    }
}
