package com.shut.telegram.profile;

import com.shut.profile.service.ProfessionalProfileService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import com.shut.user.entity.User;
import org.springframework.stereotype.Component;

/** Builds the profile page for a Telegram user from what is saved for them. */
@Component
@TelegramBotEnabled
public class ProfileScreenProvider {

    private final TelegramAccountResolver accounts;
    private final ProfessionalProfileService profiles;

    ProfileScreenProvider(TelegramAccountResolver accounts, ProfessionalProfileService profiles) {
        this.accounts = accounts;
        this.profiles = profiles;
    }

    public Screen forUser(TelegramUser from) {
        User user = accounts.resolve(from);
        return ProfileScreen.build(profiles.findByUserId(user.getId()).orElse(null), from);
    }
}
