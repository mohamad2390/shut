package com.shut.user.controller;

import com.shut.common.web.ApiPaths;
import com.shut.user.dto.UpdateUserRequest;
import com.shut.user.dto.UserOnboardingRequest;
import com.shut.user.dto.UserResponse;
import com.shut.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Local-only endpoints for exercising the user module before Telegram authentication is added. */
@RestController
@RequestMapping(ApiPaths.Users.BASE)
@Profile("local")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(ApiPaths.Users.ONBOARDING)
    public UserResponse onboard(@Valid @RequestBody UserOnboardingRequest request) {
        return UserResponse.from(userService.onboard(request));
    }

    @GetMapping(ApiPaths.Users.BY_ID)
    public UserResponse getById(@PathVariable Long id) {
        return UserResponse.from(userService.findById(id));
    }

    @PatchMapping(ApiPaths.Users.BY_ID)
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }
}
