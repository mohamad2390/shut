package com.shut.browse;

import java.util.List;

/**
 * A member in the ranking: {@code average} is 0 when nobody has rated them yet,
 * and {@code skills} are the few skills shown next to their name.
 */
public record RankedProvider(Long userId, String displayName, String city, double average, long ratings,
                             List<String> skills) {
}
