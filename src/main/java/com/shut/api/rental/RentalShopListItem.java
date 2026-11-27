package com.shut.api.rental;

/** One rental shop in the list. */
public record RentalShopListItem(Long id, String name, String city, long itemCount) {
}
