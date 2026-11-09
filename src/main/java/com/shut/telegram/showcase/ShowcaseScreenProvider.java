package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** Builds the showcase home page for a Telegram user. */
@Component
@TelegramBotEnabled
public class ShowcaseScreenProvider {

    private final TelegramAccountResolver accounts;
    private final ShowcaseService showcase;

    ShowcaseScreenProvider(TelegramAccountResolver accounts, ShowcaseService showcase) {
        this.accounts = accounts;
        this.showcase = showcase;
    }

    public Screen home(TelegramUser from) {
        Long userId = accounts.resolve(from).getId();
        return ShowcaseScreens.home(showcase.countsByCategory(userId));
    }
}
