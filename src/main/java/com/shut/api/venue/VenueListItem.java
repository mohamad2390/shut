package com.shut.api.venue;

import java.util.List;

/** One venue in the list. */
public record VenueListItem(Long id, String name, LabelResponse type, String city, List<VenuePriceResponse> prices,
                            long photoCount) {
}
