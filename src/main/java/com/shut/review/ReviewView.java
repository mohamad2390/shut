package com.shut.review;

import java.time.OffsetDateTime;

/** A review as other members read it. {@code comment} is null when only stars were given. */
public record ReviewView(String reviewerName, int rating, String comment, OffsetDateTime createdAt) {
}
