package com.shut.api.admin;

import com.shut.api.venue.LabelResponse;

/** A venue as an administrator sees it, whoever owns it and whether or not it is listed. */
public record AdminVenueItem(Long id, String name, LabelResponse type, String city, Long ownerId, String ownerName,
                             boolean active, boolean listed, int priceCount, long photoCount, long pendingRequests) {
}
