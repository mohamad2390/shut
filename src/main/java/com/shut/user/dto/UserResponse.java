package com.shut.user.dto;

import com.shut.user.entity.User;
import com.shut.user.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        Long telegramId,
        String telegramUsername,
        String firstName,
        String lastName,
        String phoneNumber,
        Set<UserRole> roles,
        boolean active,
        boolean verified,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getTelegramId(), user.getTelegramUsername(),
                user.getFirstName(), user.getLastName(), user.getPhoneNumber(),
                user.getRoles(), user.isActive(), user.isVerified(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
