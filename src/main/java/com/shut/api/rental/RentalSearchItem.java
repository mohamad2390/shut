package com.shut.api.rental;

/** One search hit: the item and the shop that rents it. */
public record RentalSearchItem(Long id, String title, String typeIcon, String typeName, long dailyPrice,
                               long photoCount, Long shopId, String shopName, String shopCity) {
}
