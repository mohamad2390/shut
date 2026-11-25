package com.shut.market;

import java.util.Optional;

/** Only an active listing (that no administrator hid) is shown to others. */
public enum ListingStatus {
    ACTIVE("فعال"),
    SOLD("فروخته شد"),
    CLOSED("بسته شد");

    private final String label;

    ListingStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ListingStatus> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
