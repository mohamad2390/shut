package com.shut.api.rental;

/** One item for rent: a catalog product with the shop's daily price. */
public record RentalItemSummary(Long id, String title, String typeIcon, String typeName, long dailyPrice,
                                long photoCount) {
}
