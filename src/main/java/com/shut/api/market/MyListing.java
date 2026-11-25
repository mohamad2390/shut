package com.shut.api.market;

import com.shut.api.venue.LabelResponse;

/** The seller's own view of a listing, whatever its status. */
public record MyListing(Long id, String title, String description, CategoryItem category, String brand, String model,
                        LabelResponse condition, long price, boolean negotiable, String city, LabelResponse status,
                        boolean hidden, long photoCount, long pendingOffers, String botUrl) {
}
