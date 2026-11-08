package com.shut.telegram.profile;

import com.shut.profile.dto.ProfessionalProfileRequest;

import java.util.Set;

/**
 * The part of a profile being changed. Fields left null are not touched by the service,
 * so a draft with one field set updates exactly that field.
 */
final class ProfileDraft {

    String displayName;
    String headline;
    String bio;
    Integer yearsExperience;
    String baseCity;
    String baseCountry;
    String websiteUrl;
    String instagramUsername;
    String equipmentDescription;
    Set<String> skills;
    Set<String> serviceCities;
    Set<String> collaborators;

    ProfessionalProfileRequest toRequest() {
        return new ProfessionalProfileRequest(
                displayName, headline, bio, yearsExperience, baseCity, baseCountry, websiteUrl,
                instagramUsername, equipmentDescription, skills, serviceCities, collaborators);
    }
}
