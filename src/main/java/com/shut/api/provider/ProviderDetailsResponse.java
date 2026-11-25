package com.shut.api.provider;

import java.util.List;
import java.util.Set;

/** A specialist's public page. Contact is the Telegram username, or null when they have none. */
public record ProviderDetailsResponse(
        Long userId,
        String displayName,
        String headline,
        String bio,
        Integer yearsExperience,
        String city,
        String country,
        String websiteUrl,
        String instagramUsername,
        String telegramUsername,
        String equipmentNotes,
        List<EquipmentItemResponse> equipment,
        Set<String> skills,
        Set<String> serviceCities,
        Set<String> collaborators,
        RatingResponse rating
) {
}
