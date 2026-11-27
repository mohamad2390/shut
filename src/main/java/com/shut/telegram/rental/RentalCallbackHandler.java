package com.shut.telegram.rental;

import com.shut.rental.RentalItem;
import com.shut.rental.RentalItemPhoto;
import com.shut.rental.RentalService;
import com.shut.rental.RentalShop;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles what anyone does with rental shops: browsing, looking at an item and its photos, starting a search. */
@Component
@TelegramBotEnabled
class RentalCallbackHandler extends CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(RentalCallbackHandler.class);
    private static final int PAGE_SIZE = 8;

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final RentalService rentals;
    private final UserService users;

    RentalCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          ConversationService conversations, RentalService rentals, UserService users) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.messenger = messenger;
        this.conversations = conversations;
        this.rentals = rentals;
        this.users = users;
    }

    @Override
    protected String prefix() {
        return RentalCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        conversations.clear(context.user().id());
        switch (context.arg(0)) {
            case "home" -> show(context, RentalScreens.home());
            case "search" -> {
                conversations.begin(context.user().id(), ConversationStep.AWAITING_RENTAL_SEARCH, Map.of());
                show(context, RentalScreens.askSearch());
            }
            case "list" -> show(context, RentalScreens.list(rentals.listed(page(context.arg(1)), PAGE_SIZE)));
            case "s" -> show(context, listedShop(context).map(this::shopPage).orElseGet(RentalScreens::gone));
            case "its" -> show(context, listedShop(context).map(shop ->
                    RentalScreens.items(shop, rentals.availableItems(shop.getId()))).orElseGet(RentalScreens::gone));
            case "i" -> show(context, publicItem(context).map(this::itemPage).orElseGet(RentalScreens::gone));
            case "ph" -> sendPhotos(context);
            default -> { }
        }
    }

    private Optional<RentalShop> listedShop(CallbackContext context) {
        return id(context.arg(1)).flatMap(rentals::findListed);
    }

    private Optional<RentalItem> publicItem(CallbackContext context) {
        return id(context.arg(1)).flatMap(rentals::publicItem);
    }

    private Screen shopPage(RentalShop shop) {
        return RentalScreens.shop(shop, rentals.availableCount(shop.getId()),
                users.findById(shop.getOwnerId()).getTelegramUsername());
    }

    private Screen itemPage(RentalItem item) {
        RentalShop shop = rentals.find(item.getShopId()).orElseThrow();
        return RentalScreens.item(item, shop, rentals.photoCount(item.getId()),
                users.findById(shop.getOwnerId()).getTelegramUsername());
    }

    /** The photos go out one by one, then the item page again so the buttons are at the bottom. */
    private void sendPhotos(CallbackContext context) {
        Optional<RentalItem> item = publicItem(context);
        if (item.isEmpty()) {
            show(context, RentalScreens.gone());
            return;
        }
        List<RentalItemPhoto> photos = rentals.photos(item.get().getId());
        if (photos.isEmpty()) {
            messenger.sendText(context.chatId(), "این مورد هنوز عکسی نداره.");
            return;
        }
        photos.forEach(photo -> sendPhoto(context.chatId(), photo));
        messenger.send(context.chatId(), itemPage(item.get()));
    }

    /** One broken file must not stop the rest from being sent. */
    private void sendPhoto(long chatId, RentalItemPhoto photo) {
        try {
            telegram.sendMedia(SendMediaRequest.photo(chatId, photo.getFileId(), null, null));
        } catch (TelegramApiException e) {
            log.warn("Could not send rental photo {}: {}", photo.getId(), e.getMessage());
        }
    }

    private static Optional<Long> id(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static int page(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
