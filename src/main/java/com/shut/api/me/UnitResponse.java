package com.shut.api.me;

/** What a price is charged per. {@code label} reads after the amount: «هر ساعت». */
public record UnitResponse(String key, String label, String icon) {
}
