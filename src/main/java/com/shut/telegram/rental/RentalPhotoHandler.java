package com.shut.telegram.rental;

import com.shut.rental.RentalItem;
import com.shut.rental.RentalService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.PhotoSize;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import com.shut.telegram.storage.MediaBackupService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalLong;

/** Receives the photos a rental owner sends for an item, and saves them. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class RentalPhotoHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final RentalService rentals;
    private final MediaBackupService backups;
    private final BotMessenger messenger;

    RentalPhotoHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, RentalService rentals, MediaBackupService backups,
                       BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.rentals = rentals;
        this.backups = backups;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.text() == null || !message.text().startsWith("/"))
                && awaitedItem(message.from().id()).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        Long ownerId = accounts.resolve(from).getId();
        RentalItem item = awaitedItem(from.id()).flatMap(id -> rentals.ownedItem(ownerId, id)).orElse(null);
        if (item == null) {
            conversations.clear(from.id());
            messenger.send(chatId, RentalOwnerScreens.gone());
            return;
        }
        if (message.photo() == null || message.photo().isEmpty()) {
            messenger.send(chatId, RentalOwnerScreens.photoRejected(item, "فقط عکس قبول می‌کنم. عکس رو بفرست یا «تمام» رو بزن."));
            return;
        }
        PhotoSize largest = message.photo().stream()
                .max(Comparator.comparingLong(size -> (long) size.width() * size.height())).orElseThrow();
        save(message, ownerId, item, largest);
    }

    private void save(Message message, Long ownerId, RentalItem item, PhotoSize photo) {
        long chatId = message.chat().id();
        RentalService.PhotoResult check = rentals.checkCanAddPhoto(ownerId, item.getId(), photo.fileUniqueId());
        if (check != RentalService.PhotoResult.ADDED) {
            messenger.send(chatId, RentalOwnerScreens.photoRejected(item, rejection(check)));
            return;
        }
        OptionalLong backupId = backups.backup(chatId, message.messageId());
        Long backupMessageId = backupId.isPresent() ? backupId.getAsLong() : null;
        RentalService.PhotoResult result = rentals.addPhoto(ownerId, item.getId(), photo.fileId(), photo.fileUniqueId(),
                backupMessageId);
        if (result != RentalService.PhotoResult.ADDED) {
            backups.remove(backupMessageId);
            messenger.send(chatId, RentalOwnerScreens.photoRejected(item, rejection(result)));
            return;
        }
        // Albums arrive as several messages at once; replying to each would flood the chat.
        if (message.mediaGroupId() == null) {
            messenger.send(chatId, RentalOwnerScreens.photoSaved(item, rentals.photoCount(item.getId())));
        }
    }

    private static String rejection(RentalService.PhotoResult result) {
        return switch (result) {
            case DUPLICATE -> "این عکس قبلاً ذخیره شده.";
            case LIMIT_REACHED -> "به حداکثر تعداد عکس رسیدی. برای عکس تازه، اول عکس‌ها رو حذف کن.";
            case NOT_FOUND -> "این مورد پیدا نشد.";
            case ADDED -> "";
        };
    }

    private Optional<Long> awaitedItem(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.AWAITING_RENTAL_PHOTO)
                .map(ConversationState::getData)
                .map(data -> data.get(RentalOwnerCallbackHandler.ITEM_KEY))
                .flatMap(raw -> {
                    try {
                        return Optional.of(Long.parseLong(raw));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
