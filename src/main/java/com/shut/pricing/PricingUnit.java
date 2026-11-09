package com.shut.pricing;

import java.util.Optional;

/** What a price is charged per. */
public enum PricingUnit {
    HOUR("هر ساعت"),
    DAY("هر روز"),
    PHOTO("هر عکس"),
    VIDEO_MINUTE("هر دقیقه ویدیو"),
    PROJECT("هر پروژه");

    private final String phrase;

    PricingUnit(String phrase) {
        this.phrase = phrase;
    }

    /** How the unit reads after a price, e.g. "هر ساعت". */
    public String phrase() {
        return phrase;
    }

    public static Optional<PricingUnit> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
