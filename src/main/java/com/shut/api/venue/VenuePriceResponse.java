package com.shut.api.venue;

/** One price of a venue. {@code title} is null when the unit says enough; the amount is in tomans. */
public record VenuePriceResponse(String title, LabelResponse unit, long amount) {
}
