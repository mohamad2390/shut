package com.shut.review;

/** How a provider is rated overall. {@code average} is 0 when there are no ratings. */
public record RatingSummary(double average, long count) {

    public static final RatingSummary NONE = new RatingSummary(0, 0);

    public boolean isEmpty() {
        return count == 0;
    }
}
