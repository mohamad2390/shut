package com.shut.profile.controller;

import com.shut.common.web.ApiPaths;
import com.shut.profile.dto.ProfessionalProfileRequest;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.profile.service.ProfessionalProfileService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.Profiles.BASE)
@Profile("local")
public class ProfessionalProfileController {

    private final ProfessionalProfileService profileService;

    public ProfessionalProfileController(ProfessionalProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfessionalProfileResponse get(@PathVariable Long userId) {
        return profileService.getByUserId(userId);
    }

    /** Upsert: creates the professional profile if it does not exist, otherwise updates it. */
    @PutMapping
    public ProfessionalProfileResponse saveOrUpdate(
            @PathVariable Long userId,
            @Valid @RequestBody ProfessionalProfileRequest request) {
        return profileService.saveOrUpdate(userId, request);
    }
}
