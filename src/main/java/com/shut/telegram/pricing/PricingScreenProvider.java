package com.shut.telegram.pricing;

import com.shut.pricing.PricingService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** Builds the price menu page for a Telegram user. */
@Component
@TelegramBotEnabled
public class PricingScreenProvider {

    private final TelegramAccountResolver accounts;
    private final PricingService pricing;

    PricingScreenProvider(TelegramAccountResolver accounts, PricingService pricing) {
        this.accounts = accounts;
        this.pricing = pricing;
    }

    public Screen home(TelegramUser from) {
        return PricingScreens.list(pricing.list(accounts.resolve(from).getId()));
    }
}
