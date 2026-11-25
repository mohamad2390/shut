package com.shut.api.me;

import com.shut.common.web.ApiPaths;
import com.shut.profile.dto.ProfessionalProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in member's own specialist profile. */
@RestController
@RequestMapping(ApiPaths.Me.BASE + ApiPaths.Me.PROFILE)
public class MyProfileController {

    private final MemberAccess access;
    private final MyProfileApiService profiles;

    public MyProfileController(MemberAccess access, MyProfileApiService profiles) {
        this.access = access;
        this.profiles = profiles;
    }

    @GetMapping
    public MyProfileResponse get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return profiles.get(access.userId(authorization));
    }

    /** Saving for the first time makes the member a specialist. */
    @PutMapping
    public MyProfileResponse save(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @Valid @RequestBody ProfessionalProfileRequest request) {
        return profiles.save(access.userId(authorization), request);
    }
}
