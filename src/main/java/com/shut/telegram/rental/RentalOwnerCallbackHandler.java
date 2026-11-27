package com.shut.telegram.rental;

import com.shut.rental.RentalItem;
import com.shut.rental.RentalItemPhoto;
import com.shut.rental.RentalService;
import com.shut.rental.RentalShop;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.storage.MediaBackupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles the rental owner's buttons: registering, details, items and their photos. */
@Component
@TelegramBotEnabled
class RentalOwnerCallbackHandler extends CallbackHandler {

    static final String FIELD_KEY = "field";
    static final String STAGE_KEY = "stage";
    static final String ITEM_KEY = "item";
    static final String MODEL_KEY = "model";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final RentalService rentals;
    private final MediaBackupService backups;

    RentalOwnerCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                               ConversationService conversations, TelegramAccountResolver accounts,
                               RentalService rentals, MediaBackupService backups) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.rentals = rentals;
        this.backups = backups;
    }

    @Override
    protected String prefix() {
        return RentalOwnerCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        conversations.clear(context.user().id());
        Long userId = accounts.resolve(context.user()).getId();
        switch (context.arg(0)) {
            case "mine" -> show(context, rentals.ownedBy(userId).map(this::manage).orElseGet(RentalOwnerScreens::intro));
            case "new" -> show(context, rentals.ownedBy(userId).map(this::manage).orElseGet(() -> {
                conversations.begin(context.user().id(), ConversationStep.AWAITING_RENTAL_WIZARD,
                        Map.of(FIELD_KEY, RentalField.NAME.name()));
                return RentalOwnerScreens.ask(RentalField.NAME, true);
            }));
            default -> withShop(context, userId);
        }
    }

    /** Everything else needs the member's shop. */
    private void withShop(CallbackContext context, Long userId) {
        Optional<RentalShop> found = rentals.ownedBy(userId);
        if (found.isEmpty()) {
            show(context, RentalOwnerScreens.intro());
            return;
        }
        RentalShop shop = found.get();
        long telegramId = context.user().id();
        Optional<Long> itemId = id(context.arg(1));
        Screen screen = switch (context.arg(0)) {
            case "e" -> RentalField.fromKey(context.arg(1)).map(field -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_RENTAL_FIELD, Map.of(FIELD_KEY, field.name()));
                return RentalOwnerScreens.ask(field, false);
            }).orElseGet(() -> manage(shop));
            case "tg" -> manage(rentals.update(userId, s -> s.setActive(!s.isActive())).orElse(shop));
            case "its" -> RentalOwnerScreens.items(rentals.items(shop.getId()));
            case "i" -> withItem(userId, itemId, this::itemScreen);
            case "it" -> withItem(userId, itemId, item ->
                    itemScreen(rentals.updateItem(userId, item.getId(), i -> i.setAvailable(!i.isAvailable())).orElse(item)));
            case "ip" -> withItem(userId, itemId, item -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_RENTAL_ITEM,
                        Map.of(STAGE_KEY, "EDIT_PRICE", ITEM_KEY, String.valueOf(item.getId())));
                return RentalOwnerScreens.askNewPrice(item);
            });
            case "ph" -> withItem(userId, itemId, item -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_RENTAL_PHOTO,
                        Map.of(ITEM_KEY, String.valueOf(item.getId())));
                return RentalOwnerScreens.addPhotos(item, rentals.photoCount(item.getId()));
            });
            case "phx" -> withItem(userId, itemId, item -> {
                removeBackups(rentals.clearPhotos(userId, item.getId()));
                return itemScreen(item);
            });
            case "irm" -> {
                itemId.flatMap(id -> rentals.removeItem(userId, id)).ifPresent(this::removeBackups);
                yield RentalOwnerScreens.items(rentals.items(shop.getId()));
            }
            case "del" -> RentalOwnerScreens.confirmDelete(shop);
            case "delok" -> {
                rentals.delete(userId).ifPresent(this::removeBackups);
                yield RentalOwnerScreens.intro();
            }
            default -> null;
        };
        if (screen != null) {
            show(context, screen);
        }
    }

    private Screen withItem(Long userId, Optional<Long> itemId, java.util.function.Function<RentalItem, Screen> action) {
        return itemId.flatMap(id -> rentals.ownedItem(userId, id)).map(action).orElseGet(RentalOwnerScreens::gone);
    }

    private Screen itemScreen(RentalItem item) {
        return RentalOwnerScreens.item(item, rentals.photoCount(item.getId()));
    }

    private Screen manage(RentalShop shop) {
        return RentalOwnerScreens.manage(shop, rentals.isListed(shop), rentals.items(shop.getId()).size(),
                rentals.availableCount(shop.getId()));
    }

    private void removeBackups(List<RentalItemPhoto> photos) {
        photos.forEach(photo -> backups.remove(photo.getBackupMessageId()));
    }

    private static Optional<Long> id(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
