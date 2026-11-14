package com.shut.user.dto;

import com.shut.user.entity.User;
import com.shut.user.entity.Role;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.TreeSet;

public record UserResponse(
        Long id,
        Long telegramId,
        String telegramUsername,
        String firstName,
        String lastName,
        String phoneNumber,
        Set<String> roles,
        boolean active,
        boolean verified,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getTelegramId(), user.getTelegramUsername(),
                user.getFirstName(), user.getLastName(), user.getPhoneNumber(),
                new TreeSet<>(user.getRoles().stream().map(Role::getCode).toList()), user.isActive(), user.isVerified(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
