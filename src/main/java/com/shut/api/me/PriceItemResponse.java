package com.shut.api.me;

/** One line of the price menu. {@code amount} is in tomans; {@code serviceId} is null for a price typed by hand. */
public record PriceItemResponse(Long id, String title, String icon, Long serviceId, UnitResponse unit, long amount) {
}
