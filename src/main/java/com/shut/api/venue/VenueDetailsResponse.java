package com.shut.api.venue;

import java.util.List;

/** A venue's page. {@code ownerTelegramUsername} and {@code phone} are null when the owner gave none. */
public record VenueDetailsResponse(
        Long id,
        String name,
        LabelResponse type,
        String city,
        String address,
        String description,
        String amenities,
        String phone,
        String ownerTelegramUsername,
        List<VenuePriceResponse> prices,
        List<VenuePhotoResponse> photos
) {
}
