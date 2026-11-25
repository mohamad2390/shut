package com.shut.api.me.venue;

import com.shut.api.venue.LabelResponse;

/** One of the member's venues in their list. {@code listed}: other members can see it. */
public record MyVenueSummary(Long id, String name, LabelResponse type, String city, boolean active, boolean listed,
                             long pendingRequests, int priceCount, long photoCount) {
}
