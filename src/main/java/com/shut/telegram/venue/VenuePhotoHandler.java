package com.shut.telegram.venue;

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
import com.shut.venue.Venue;
import com.shut.venue.VenueService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalLong;

/** Receives the photos an owner sends for a venue, and saves them. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class VenuePhotoHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final VenueService venues;
    private final MediaBackupService backups;
    private final BotMessenger messenger;

    VenuePhotoHandler(ConversationService conversations, MembershipGate membershipGate,
                      TelegramAccountResolver accounts, VenueService venues, MediaBackupService backups,
                      BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.venues = venues;
        this.backups = backups;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.text() == null || !message.text().startsWith("/"))
                && awaitedVenue(message.from().id()).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        Long venueId = awaitedVenue(from.id()).orElse(null);
        Venue venue = venueId == null ? null : venues.findOwned(venueId, accounts.resolve(from).getId()).orElse(null);
        if (venue == null) {
            conversations.clear(from.id());
            messenger.send(chatId, VenueOwnerScreens.gone());
            return;
        }
        if (message.photo() == null || message.photo().isEmpty()) {
            messenger.send(chatId, VenueOwnerScreens.photoRejected(venue, "فقط عکس قبول می‌کنم. عکس رو بفرست یا «تمام» رو بزن."));
            return;
        }
        PhotoSize largest = message.photo().stream()
                .max(Comparator.comparingLong(size -> (long) size.width() * size.height()))
                .orElseThrow();
        save(message, venue, largest);
    }

    private void save(Message message, Venue venue, PhotoSize photo) {
        long chatId = message.chat().id();
        Long ownerId = venue.getOwnerId();
        VenueService.PhotoResult check = venues.checkCanAddPhoto(venue.getId(), ownerId, photo.fileUniqueId());
        if (check != VenueService.PhotoResult.ADDED) {
            messenger.send(chatId, VenueOwnerScreens.photoRejected(venue, rejection(check)));
            return;
        }
        OptionalLong backupId = backups.backup(chatId, message.messageId());
        Long backupMessageId = backupId.isPresent() ? backupId.getAsLong() : null;
        VenueService.PhotoResult result = venues.addPhoto(venue.getId(), ownerId, photo.fileId(), photo.fileUniqueId(),
                backupMessageId);
        if (result != VenueService.PhotoResult.ADDED) {
            backups.remove(backupMessageId);
            messenger.send(chatId, VenueOwnerScreens.photoRejected(venue, rejection(result)));
            return;
        }
        // Albums arrive as several messages at once; replying to each would flood the chat.
        if (message.mediaGroupId() == null) {
            messenger.send(chatId, VenueOwnerScreens.photoSaved(venue, venues.photoCount(venue.getId())));
        }
    }

    private static String rejection(VenueService.PhotoResult result) {
        return switch (result) {
            case DUPLICATE -> "این عکس قبلاً برای این مکان ذخیره شده.";
            case LIMIT_REACHED -> "به حداکثر تعداد عکس رسیدی. برای عکس تازه، اول عکس‌ها رو حذف کن.";
            case NOT_FOUND -> "این مکان پیدا نشد.";
            case ADDED -> "";
        };
    }

    private Optional<Long> awaitedVenue(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.AWAITING_VENUE_PHOTO)
                .map(ConversationState::getData)
                .map(data -> data.get(VenueCallbackHandler.VENUE_KEY))
                .flatMap(VenueArgs::id);
    }
}
