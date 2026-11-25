package com.shut.api.me;

/** A new price: a service from the catalog, a unit key such as {@code HOUR}, and tomans. {@code title} is optional. */
public record CreatePriceRequest(Long serviceId, String title, String unit, Long amount) {
}
