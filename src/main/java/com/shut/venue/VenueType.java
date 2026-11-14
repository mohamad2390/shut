package com.shut.venue;

import java.util.Arrays;
import java.util.Optional;

/** What kind of place a venue is. */
public enum VenueType {
    MANSION("عمارت", "🏛"),
    GARDEN("باغ", "🌳"),
    VILLA("ویلا", "🏡"),
    STUDIO("استودیو", "🎞"),
    OTHER("مکان دیگر", "📍");

    private final String label;
    private final String icon;

    VenueType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() { return label; }
    public String icon() { return icon; }

    public static Optional<VenueType> fromKey(String key) {
        return Arrays.stream(values()).filter(type -> type.name().equals(key)).findFirst();
    }
}
