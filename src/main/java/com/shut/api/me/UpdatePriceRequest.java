package com.shut.api.me;

/** What to change on a price; leave out what stays the same. */
public record UpdatePriceRequest(String unit, Long amount) {
}
