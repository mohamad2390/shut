package com.shut.profile.service;

import com.shut.profile.dto.ProfessionalProfileRequest;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.profile.entity.ProfessionalProfile;
import com.shut.profile.exception.ProfessionalProfileNotFoundException;
import com.shut.profile.repository.ProfessionalProfileRepository;
import com.shut.user.entity.User;
import com.shut.user.entity.UserRole;
import com.shut.user.exception.UserNotFoundException;
import com.shut.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfessionalProfileService(ProfessionalProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfessionalProfileResponse getByUserId(Long userId) {
        ProfessionalProfile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ProfessionalProfileNotFoundException(userId));
        return ProfessionalProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public Optional<ProfessionalProfileResponse> findByUserId(Long userId) {
        return profileRepository.findByUser_Id(userId).map(ProfessionalProfileResponse::from);
    }

    /** Creates the profile on first save; subsequent calls replace the submitted profile fields. */
    @Transactional
    public ProfessionalProfileResponse saveOrUpdate(Long userId, ProfessionalProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        ProfessionalProfile profile = profileRepository.findByUser_Id(userId)
                .orElseGet(() -> new ProfessionalProfile(user));

        if (request.displayName() != null) profile.setDisplayName(clean(request.displayName()));
        if (request.headline() != null) profile.setHeadline(clean(request.headline()));
        if (request.bio() != null) profile.setBio(clean(request.bio()));
        if (request.yearsExperience() != null) profile.setYearsExperience(request.yearsExperience());
        if (request.baseCity() != null) profile.setBaseCity(clean(request.baseCity()));
        if (request.baseCountry() != null) profile.setBaseCountry(clean(request.baseCountry()));
        if (request.websiteUrl() != null) profile.setWebsiteUrl(clean(request.websiteUrl()));
        if (request.instagramUsername() != null) profile.setInstagramUsername(clean(request.instagramUsername()));
        if (request.equipmentDescription() != null) profile.setEquipmentDescription(clean(request.equipmentDescription()));
        if (request.skills() != null) profile.setSkills(cleanSet(request.skills()));
        if (request.serviceCities() != null) profile.setServiceCities(cleanSet(request.serviceCities()));
        if (request.collaborators() != null) profile.setCollaborators(cleanSet(request.collaborators()));

        user.addRole(UserRole.PROVIDER);
        ProfessionalProfile saved = profileRepository.save(profile);
        return ProfessionalProfileResponse.from(saved);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Set<String> cleanSet(Set<String> values) {
        if (values == null) return Set.of();
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
