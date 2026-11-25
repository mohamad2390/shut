package com.shut.api.me;

/** A service a price can be for. {@code asksName}: the member may name the price themselves («سایر»). */
public record ServiceTypeResponse(Long id, String name, String icon, boolean asksName) {
}
