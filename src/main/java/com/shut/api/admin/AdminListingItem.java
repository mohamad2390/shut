package com.shut.api.admin;

import com.shut.api.venue.LabelResponse;

/** A listing as an administrator sees it, whoever sells it and whatever its status. */
public record AdminListingItem(Long id, String title, long price, LabelResponse status, boolean hidden, Long sellerId,
                               String sellerName, long photoCount, long pendingOffers) {
}
