package com.shut.telegram.project;

import com.shut.browse.ProviderSearchService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Component;

/** How members are named on project screens: their profile name if they have one, else their Telegram name. */
@Component
@TelegramBotEnabled
class MemberNames {

    private static final String ANONYMOUS = "یک عضو";

    private final UserService users;
    private final ProviderSearchService providers;

    MemberNames(UserService users, ProviderSearchService providers) {
        this.users = users;
        this.providers = providers;
    }

    String of(long userId) {
        return providers.find(userId)
                .map(details -> details.profile().displayName())
                .orElseGet(() -> telegramName(users.findById(userId)));
    }

    /** The member's Telegram username, or null if they have none. */
    String usernameOf(long userId) {
        return users.findById(userId).getTelegramUsername();
    }

    private static String telegramName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? ANONYMOUS : name;
    }
}
