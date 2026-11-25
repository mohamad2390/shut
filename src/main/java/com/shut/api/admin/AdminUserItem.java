package com.shut.api.admin;

import com.shut.user.entity.Role;
import com.shut.user.entity.User;

import java.time.OffsetDateTime;
import java.util.List;

/** An account as an administrator sees it, with the contact details that are hidden from everyone else. */
public record AdminUserItem(Long id, String name, String phone, String telegramUsername, boolean telegramLinked,
                            List<String> roles, boolean active, OffsetDateTime createdAt) {

    static AdminUserItem of(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new AdminUserItem(user.getId(), name.isEmpty() ? null : name, user.getPhoneNumber(),
                user.getTelegramUsername(), user.getTelegramId() != null,
                user.getRoles().stream().map(Role::getCode).sorted().toList(), user.isActive(), user.getCreatedAt());
    }
}
