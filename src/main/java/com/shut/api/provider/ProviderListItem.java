package com.shut.api.provider;

import java.util.List;

/** One specialist in the list: name, city, rating and up to three skills. */
public record ProviderListItem(Long userId, String displayName, String city, RatingResponse rating, List<String> skills) {
}
