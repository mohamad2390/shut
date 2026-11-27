package com.shut.api.rental;

import java.util.List;

/** One item with the shop's contact details and its photo ids. */
public record RentalItemDetailsResponse(Long id, String title, String typeIcon, String typeName, String description,
                                        long dailyPrice, Long shopId, String shopName, String shopPhone,
                                        String shopCity, String shopAddress, String ownerTelegramUsername,
                                        List<Long> photoIds) {
}
