package com.shut.telegram.market;

import com.shut.market.Listing;
import com.shut.market.MarketService;
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

/** Receives the photos a seller sends for a listing, and saves them. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class MarketPhotoHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final MarketService market;
    private final MediaBackupService backups;
    private final BotMessenger messenger;

    MarketPhotoHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, MarketService market, MediaBackupService backups,
                       BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.market = market;
        this.backups = backups;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.text() == null || !message.text().startsWith("/"))
                && awaitedListing(message.from().id()).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        Long listingId = awaitedListing(from.id()).orElse(null);
        Listing listing = listingId == null ? null
                : market.findOwned(listingId, accounts.resolve(from).getId()).orElse(null);
        if (listing == null) {
            conversations.clear(from.id());
            messenger.send(chatId, MarketOwnerScreens.gone());
            return;
        }
        if (message.photo() == null || message.photo().isEmpty()) {
            messenger.send(chatId, MarketOwnerScreens.photoRejected(listing, "فقط عکس قبول می‌کنم. عکس رو بفرست یا «تمام» رو بزن."));
            return;
        }
        PhotoSize largest = message.photo().stream()
                .max(Comparator.comparingLong(size -> (long) size.width() * size.height()))
                .orElseThrow();
        save(message, listing, largest);
    }

    private void save(Message message, Listing listing, PhotoSize photo) {
        long chatId = message.chat().id();
        Long sellerId = listing.getSellerId();
        MarketService.PhotoResult check = market.checkCanAddPhoto(listing.getId(), sellerId, photo.fileUniqueId());
        if (check != MarketService.PhotoResult.ADDED) {
            messenger.send(chatId, MarketOwnerScreens.photoRejected(listing, rejection(check)));
            return;
        }
        OptionalLong backupId = backups.backup(chatId, message.messageId());
        Long backupMessageId = backupId.isPresent() ? backupId.getAsLong() : null;
        MarketService.PhotoResult result = market.addPhoto(listing.getId(), sellerId, photo.fileId(),
                photo.fileUniqueId(), backupMessageId);
        if (result != MarketService.PhotoResult.ADDED) {
            backups.remove(backupMessageId);
            messenger.send(chatId, MarketOwnerScreens.photoRejected(listing, rejection(result)));
            return;
        }
        // Albums arrive as several messages at once; replying to each would flood the chat.
        if (message.mediaGroupId() == null) {
            messenger.send(chatId, MarketOwnerScreens.photoSaved(listing, market.photoCount(listing.getId())));
        }
    }

    private static String rejection(MarketService.PhotoResult result) {
        return switch (result) {
            case DUPLICATE -> "این عکس قبلاً برای این آگهی ذخیره شده.";
            case LIMIT_REACHED -> "به حداکثر تعداد عکس رسیدی. برای عکس تازه، اول عکس‌ها رو حذف کن.";
            case NOT_FOUND -> "این آگهی پیدا نشد.";
            case ADDED -> "";
        };
    }

    private Optional<Long> awaitedListing(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.AWAITING_LISTING_PHOTO)
                .map(ConversationState::getData)
                .map(data -> data.get(MarketCallbackHandler.LISTING_KEY))
                .flatMap(raw -> {
                    try {
                        return Optional.of(Long.parseLong(raw));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
