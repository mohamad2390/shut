package com.shut.browse;

import com.shut.profile.dto.ProfessionalProfileResponse;

/** A member's public page. {@code telegramUsername} is null when they have none, so they cannot be linked to. */
public record ProviderDetails(ProfessionalProfileResponse profile, String telegramUsername) {

    public Long userId() {
        return profile.userId();
    }
}
