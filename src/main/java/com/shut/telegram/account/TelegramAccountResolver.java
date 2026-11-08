package com.shut.telegram.account;

import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.TelegramUser;
import com.shut.user.dto.UserOnboardingRequest;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Component;

/** Maps a Telegram user to the application account, creating the account on first contact. */
@Component
@TelegramBotEnabled
public class TelegramAccountResolver {

    private final UserService userService;

    public TelegramAccountResolver(UserService userService) {
        this.userService = userService;
    }

    public User resolve(TelegramUser telegramUser) {
        return userService.onboard(new UserOnboardingRequest(
                telegramUser.id(), telegramUser.username(), telegramUser.firstName(), telegramUser.lastName()));
    }
}
