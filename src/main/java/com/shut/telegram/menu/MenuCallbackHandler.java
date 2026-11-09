package com.shut.telegram.menu;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.handler.BotMessages;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.calendar.CalendarScreenProvider;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.pricing.PricingScreenProvider;
import com.shut.telegram.profile.ProfileScreenProvider;
import com.shut.telegram.showcase.ShowcaseScreenProvider;
import org.springframework.stereotype.Component;

/** Handles presses on the main menu buttons by showing the matching section. */
@Component
@TelegramBotEnabled
class MenuCallbackHandler extends CallbackHandler {

    private final BotMessenger messenger;
    private final ProfileScreenProvider profileScreens;
    private final ShowcaseScreenProvider showcaseScreens;
    private final PricingScreenProvider pricingScreens;
    private final CalendarScreenProvider calendarScreens;

    MenuCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                        BotMessenger messenger, ProfileScreenProvider profileScreens,
                        ShowcaseScreenProvider showcaseScreens, PricingScreenProvider pricingScreens,
                        CalendarScreenProvider calendarScreens) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.profileScreens = profileScreens;
        this.showcaseScreens = showcaseScreens;
        this.pricingScreens = pricingScreens;
        this.calendarScreens = calendarScreens;
    }

    @Override
    protected String prefix() {
        return MenuCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        Screen screen = switch (context.arg(0)) {
            case "home" -> MainMenuScreen.build(BotMessages.MENU_INTRO);
            case "profile" -> profileScreens.forUser(context.user());
            case "showcase" -> showcaseScreens.home(context.user());
            case "calendar" -> calendarScreens.current(context.user());
            case "pricing" -> pricingScreens.home(context.user());
            default -> null;
        };
        if (screen != null) {
            messenger.show(context.chatId(), context.messageId(), screen);
        }
    }
}
