package com.shut.market;

import java.util.Optional;

/** How worn the gear is. */
public enum ListingCondition {
    NEW("نو و آکبند"),
    LIKE_NEW("در حد نو"),
    USED("کارکرده، سالم"),
    NEEDS_REPAIR("نیاز به تعمیر");

    private final String label;

    ListingCondition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ListingCondition> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
