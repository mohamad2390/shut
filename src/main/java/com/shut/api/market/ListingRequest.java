package com.shut.api.market;

/**
 * A listing to create (title, condition and price are required) or change (a field that is not sent stays as it is,
 * an empty text clears an optional field, a {@code categoryId} of 0 clears the category).
 */
public record ListingRequest(String title, Long categoryId, String brand, String model, String condition, Long price,
                             Boolean negotiable, String city, String description) {
}
