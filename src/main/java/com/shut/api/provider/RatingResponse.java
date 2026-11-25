package com.shut.api.provider;

import com.shut.review.RatingSummary;

/** The average of the ratings a member received and how many there are; both 0 when nobody rated them. */
public record RatingResponse(double average, long count) {

    static RatingResponse of(RatingSummary summary) {
        return new RatingResponse(summary.average(), summary.count());
    }
}
