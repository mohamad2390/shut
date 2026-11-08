package com.shut.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserOnboardingRequest(
        @NotNull Long telegramId,
        @Size(max = 100) String telegramUsername,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName
) {
}
