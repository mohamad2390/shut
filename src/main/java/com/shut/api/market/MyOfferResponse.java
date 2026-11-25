package com.shut.api.market;

import com.shut.api.venue.LabelResponse;

import java.time.OffsetDateTime;

/** An offer the member made. {@code seller} is only there once the seller accepted. */
public record MyOfferResponse(Long id, Long listingId, String listingTitle, Long amount, String message,
                              LabelResponse status, OffsetDateTime createdAt, ContactResponse seller) {
}
