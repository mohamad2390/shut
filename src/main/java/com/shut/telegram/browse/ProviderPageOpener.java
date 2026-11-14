package com.shut.telegram.browse;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.equipment.EquipmentService;
import com.shut.review.ReviewService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Opens a member's page in a chat straight from a link, without going through search. */
@Component
@TelegramBotEnabled
public class ProviderPageOpener {

    private final ProviderSearchService providers;
    private final ReviewService reviews;
    private final EquipmentService equipment;
    private final BotMessenger messenger;

    ProviderPageOpener(ProviderSearchService providers, ReviewService reviews, EquipmentService equipment,
                       BotMessenger messenger) {
        this.providers = providers;
        this.reviews = reviews;
        this.equipment = equipment;
        this.messenger = messenger;
    }

    /** Sends the member's page. False means there is no such member, and nothing was sent. */
    public boolean open(long chatId, long userId) {
        Optional<ProviderDetails> details = providers.find(userId);
        if (details.isEmpty()) {
            return false;
        }
        messenger.send(chatId, BrowseScreens.provider(details.get(), reviews.summary(userId),
                equipment.lines(userId), InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME)));
        return true;
    }
}
