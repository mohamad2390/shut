package com.shut.api.market;

/** An offer: {@code amount} in tomans, or null to take it at the asking price. */
public record OfferRequest(Long amount, String message) {
}
