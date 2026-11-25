package com.shut.telegram.studio;

import com.shut.studio.MediaKind;
import com.shut.studio.StudioService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
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
import java.util.OptionalLong;

/** Receives the photos and videos a studio owner sends for the showcase, and saves them. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class StudioMediaHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final StudioService studios;
    private final MediaBackupService backups;
    private final BotMessenger messenger;

    StudioMediaHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, StudioService studios, MediaBackupService backups,
                       BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.studios = studios;
        this.backups = backups;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.text() == null || !message.text().startsWith("/"))
                && conversations.find(message.from().id())
                        .filter(state -> state.getStep() == ConversationStep.AWAITING_STUDIO_MEDIA).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        boolean photo = message.photo() != null && !message.photo().isEmpty();
        if (!photo && message.video() == null) {
            messenger.send(chatId, StudioOwnerScreens.mediaRejected("فقط عکس یا ویدیو قبول می‌کنم. بفرست یا «تمام» رو بزن."));
            return;
        }
        MediaKind kind = photo ? MediaKind.PHOTO : MediaKind.VIDEO;
        String fileId;
        String uniqueId;
        if (photo) {
            PhotoSize largest = message.photo().stream()
                    .max(Comparator.comparingLong(size -> (long) size.width() * size.height())).orElseThrow();
            fileId = largest.fileId();
            uniqueId = largest.fileUniqueId();
        } else {
            fileId = message.video().fileId();
            uniqueId = message.video().fileUniqueId();
        }
        save(message, accounts.resolve(from).getId(), kind, fileId, uniqueId);
    }

    private void save(Message message, Long ownerId, MediaKind kind, String fileId, String uniqueId) {
        long chatId = message.chat().id();
        StudioService.MediaResult check = studios.checkCanAddMedia(ownerId, uniqueId);
        if (check != StudioService.MediaResult.ADDED) {
            messenger.send(chatId, StudioOwnerScreens.mediaRejected(rejection(check)));
            return;
        }
        OptionalLong backupId = backups.backup(chatId, message.messageId());
        Long backupMessageId = backupId.isPresent() ? backupId.getAsLong() : null;
        StudioService.MediaResult result = studios.addMedia(ownerId, kind, fileId, uniqueId, backupMessageId);
        if (result != StudioService.MediaResult.ADDED) {
            backups.remove(backupMessageId);
            messenger.send(chatId, StudioOwnerScreens.mediaRejected(rejection(result)));
            return;
        }
        // Albums arrive as several messages at once; replying to each would flood the chat.
        if (message.mediaGroupId() == null) {
            studios.ownedBy(ownerId).ifPresent(studio ->
                    messenger.send(chatId, StudioOwnerScreens.mediaSaved(studios.mediaCount(studio.getId()))));
        }
    }

    private static String rejection(StudioService.MediaResult result) {
        return switch (result) {
            case DUPLICATE -> "این مورد قبلاً اضافه شده.";
            case LIMIT_REACHED -> "به حداکثر تعداد رسیدی. برای مورد تازه، اول نمونه‌کارها رو حذف کن.";
            case NOT_FOUND -> "استودیویی پیدا نشد.";
            case ADDED -> "";
        };
    }
}
