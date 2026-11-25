package com.shut.api.admin;

/** A unit a price can be charged per. The units themselves are fixed; only the icon can be changed. */
public record AdminUnitItem(String key, String label, String icon) {
}
