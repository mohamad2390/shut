package com.shut.api.me.venue;

/**
 * What to change on a venue. A field left out stays as it is; an empty text clears it (except the name). {@code type}
 * is a key such as {@code GARDEN}.
 */
public record UpdateVenueRequest(String name, String type, String city, String address, String description,
                                 String amenities, String phone, Boolean active) {
}
