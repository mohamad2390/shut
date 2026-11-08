package com.shut.profile.dto;

import com.shut.profile.entity.ProfessionalProfile;

import java.time.OffsetDateTime;
import java.util.Set;

public record ProfessionalProfileResponse(
        Long id,
        Long userId,
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
        Set<String> collaborators,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProfessionalProfileResponse from(ProfessionalProfile profile) {
        return new ProfessionalProfileResponse(
                profile.getId(), profile.getUser().getId(), profile.getDisplayName(),
                profile.getHeadline(), profile.getBio(), profile.getYearsExperience(),
                profile.getBaseCity(), profile.getBaseCountry(), profile.getWebsiteUrl(),
                profile.getInstagramUsername(), profile.getEquipmentDescription(),
                profile.getSkills(), profile.getServiceCities(), profile.getCollaborators(),
                profile.getCreatedAt(), profile.getUpdatedAt()
        );
    }
}
