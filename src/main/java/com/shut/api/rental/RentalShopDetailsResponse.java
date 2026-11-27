package com.shut.api.rental;

import java.util.List;

/** A shop with its available items. */
public record RentalShopDetailsResponse(Long id, String name, String phone, String city, String address,
                                        String description, String ownerTelegramUsername,
                                        List<RentalItemSummary> items) {
}
