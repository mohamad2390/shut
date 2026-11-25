package com.shut.api.auth;

import com.shut.user.entity.User;

/** The signed-in member. {@code telegramLinked} is false for an account that has only used the web. */
public record MeResponse(Long id, String displayName, String phone, boolean telegramLinked, boolean provider) {

    static MeResponse of(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new MeResponse(user.getId(), name.isEmpty() ? null : name, user.getPhoneNumber(),
                user.getTelegramId() != null, user.isProvider());
    }
}
