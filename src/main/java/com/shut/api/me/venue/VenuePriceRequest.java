package com.shut.api.me.venue;

/** A price of a venue: a unit key such as {@code HOUR}, tomans, and an optional name («خصوصی»). */
public record VenuePriceRequest(String unit, String title, Long amount) {
}
