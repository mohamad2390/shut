package com.shut.api.me;

import com.shut.profile.dto.ProfessionalProfileResponse;

import java.util.Set;

/** The member's own specialist profile. {@code registered} is false until they save it for the first time. */
public record MyProfileResponse(
        boolean registered,
        String displayName,
        String headline,
        String bio,
        Integer yearsExperience,
        String baseCity,
        String baseCountry,
        String websiteUrl,
        String instagramUsername,
        String equipmentDescription,
        Set<String> skills,
        Set<String> serviceCities,
        Set<String> collaborators
) {

    static final MyProfileResponse NOT_REGISTERED = new MyProfileResponse(
            false, null, null, null, null, null, null, null, null, null, Set.of(), Set.of(), Set.of());

    static MyProfileResponse of(ProfessionalProfileResponse profile) {
        return new MyProfileResponse(true, profile.displayName(), profile.headline(), profile.bio(),
                profile.yearsExperience(), profile.baseCity(), profile.baseCountry(), profile.websiteUrl(),
                profile.instagramUsername(), profile.equipmentDescription(), profile.skills(),
                profile.serviceCities(), profile.collaborators());
    }
}
