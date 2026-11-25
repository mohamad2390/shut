package com.shut.api.market;

import com.shut.api.venue.LabelResponse;

public record ListingListItem(Long id, String title, LabelResponse condition, String category, String brand,
                              String model, long price, boolean negotiable, String city, Long firstPhotoId) {
}
