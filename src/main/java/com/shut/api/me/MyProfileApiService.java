package com.shut.api.me;

import com.shut.api.BadRequestException;
import com.shut.profile.dto.ProfessionalProfileRequest;
import com.shut.profile.service.ProfessionalProfileService;
import org.springframework.stereotype.Service;

/** The member's own profile for the web app; saving it is what makes them a specialist, as in the bot. */
@Service
public class MyProfileApiService {

    private final ProfessionalProfileService profiles;

    public MyProfileApiService(ProfessionalProfileService profiles) {
        this.profiles = profiles;
    }

    public MyProfileResponse get(Long userId) {
        return profiles.findByUserId(userId).map(MyProfileResponse::of).orElse(MyProfileResponse.NOT_REGISTERED);
    }

    /**
     * Saves the fields that are sent (an empty text clears a field; a missing one is left alone).
     *
     * @throws BadRequestException if the profile would end up without a display name
     */
    public MyProfileResponse save(Long userId, ProfessionalProfileRequest request) {
        boolean registered = profiles.findByUserId(userId).isPresent();
        String name = request.displayName();
        if ((name == null && !registered) || (name != null && name.isBlank())) {
            throw new BadRequestException("A display name is required");
        }
        return MyProfileResponse.of(profiles.saveOrUpdate(userId, request));
    }
}
