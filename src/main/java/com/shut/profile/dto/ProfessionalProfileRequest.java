package com.shut.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ProfessionalProfileRequest(
        @Size(max = 150) String displayName,
        @Size(max = 180) String headline,
        @Size(max = 5000) String bio,
        @Min(0) @Max(80) Integer yearsExperience,
        @Size(max = 100) String baseCity,
        @Size(max = 100) String baseCountry,
        @Size(max = 500) String websiteUrl,
        @Size(max = 100) String instagramUsername,
        @Size(max = 3000) String equipmentDescription,
        @Size(max = 40) Set<@Size(max = 100) String> skills,
        @Size(max = 40) Set<@Size(max = 100) String> serviceCities,
        @Size(max = 40) Set<@Size(max = 150) String> collaborators
) { }
