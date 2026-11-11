package com.shut.review;

/** A member tried to rate themselves. */
public class SelfReviewException extends RuntimeException {

    public SelfReviewException() {
        super("A member cannot review themselves");
    }
}
