package com.shut.api.me.venue;

import com.shut.api.venue.LabelResponse;

import java.util.List;

/**
 * A venue as its owner edits it. {@code missing} says what stops it from being listed: {@code CITY} or {@code PRICE}
 * (a venue that is switched off, {@code active} false, is not listed either). Photos are added in the bot only;
 * {@code botUrl} opens it.
 */
public record MyVenueDetails(Long id, String name, LabelResponse type, String city, String address,
                             String description, String amenities, String phone, boolean active, boolean listed,
                             List<String> missing, List<Price> prices, long photoCount, String botUrl) {

    /** One price of the venue. */
    public record Price(Long id, String title, LabelResponse unit, long amount) {
    }
}
