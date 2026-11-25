package com.shut.api.market;

import com.shut.api.venue.LabelResponse;

import java.time.OffsetDateTime;
import java.util.List;

/** A listing others can see. The seller's contact is not here: they give it with an accepted offer. */
public record ListingDetails(Long id, String title, String description, CategoryItem category, String brand,
                             String model, LabelResponse condition, long price, boolean negotiable, String city,
                             String sellerName, List<Long> photoIds, OffsetDateTime createdAt) {
}
