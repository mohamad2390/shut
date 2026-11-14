package com.shut.venue;

import java.util.Arrays;
import java.util.Optional;

/** What a venue's price is for. */
public enum VenuePriceUnit {
    HOUR("ساعتی", "هر ساعت"),
    HALF_DAY("نیم‌روزی", "هر نیم‌روز"),
    DAY("روزی", "هر روز"),
    FIXED("یک‌جا", "یک‌جا");

    private final String label;
    private final String phrase;

    VenuePriceUnit(String label, String phrase) {
        this.label = label;
        this.phrase = phrase;
    }

    public String label() { return label; }
    public String phrase() { return phrase; }

    public static Optional<VenuePriceUnit> fromKey(String key) {
        return Arrays.stream(values()).filter(unit -> unit.name().equals(key)).findFirst();
    }
}
