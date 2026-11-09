package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseItemRejectedException;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import com.shut.telegram.storage.MediaBackupService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.OptionalLong;

/** Receives the files a user sends while adding portfolio items, and saves them. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class ShowcaseMediaHandler implements UpdateHandler {

    private static final int MAX_CAPTION_LENGTH = 1024;

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final ShowcaseService showcase;
    private final MediaBackupService backups;
    private final BotMessenger messenger;

    ShowcaseMediaHandler(ConversationService conversations, MembershipGate membershipGate,
                         TelegramAccountResolver accounts, ShowcaseService showcase,
                         MediaBackupService backups, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.showcase = showcase;
        this.backups = backups;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.text() == null || !message.text().startsWith("/"))
                && awaitedCategory(message.from().id()).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();

        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        ShowcaseCategory category = awaitedCategory(from.id()).orElse(null);
        if (category == null) {
            return;
        }

        Optional<ReceivedMedia> media = ReceivedMedia.from(message);
        if (media.isEmpty()) {
            messenger.send(chatId, new Screen("فقط عکس یا ویدیو قبول می‌کنم. فایل رو بفرست یا «تمام» رو بزن.",
                    ShowcaseScreens.finishKeyboard(category)));
            return;
        }
        save(message, from, category, media.get());
    }

    private void save(Message message, TelegramUser from, ShowcaseCategory category, ReceivedMedia media) {
        long chatId = message.chat().id();
        Long userId = accounts.resolve(from).getId();

        try {
            showcase.checkCanAdd(userId, category, media.fileUniqueId());
        } catch (ShowcaseItemRejectedException e) {
            replyRejected(chatId, category, e);
            return;
        }

        OptionalLong backupId = backups.backup(chatId, message.messageId());
        Long backupMessageId = backupId.isPresent() ? backupId.getAsLong() : null;
        try {
            showcase.add(userId, category, media.type(), media.fileId(), media.fileUniqueId(),
                    caption(message), backupMessageId);
        } catch (ShowcaseItemRejectedException e) {
            backups.remove(backupMessageId);
            replyRejected(chatId, category, e);
            return;
        }

        // Albums arrive as several messages at once; replying to each would flood the chat.
        if (message.mediaGroupId() == null) {
            messenger.send(chatId, new Screen("✅ ذخیره شد. فایل بعدی رو بفرست یا «تمام» رو بزن.",
                    ShowcaseScreens.finishKeyboard(category)));
        }
    }

    private void replyRejected(long chatId, ShowcaseCategory category, ShowcaseItemRejectedException e) {
        String text = switch (e.reason()) {
            case DUPLICATE -> "این فایل قبلاً توی ویترینت هست.";
            case LIMIT_REACHED -> "این دسته پر شد. برای اضافه کردن، اول چندتا از نمونه‌کارها رو حذف کن.";
        };
        messenger.send(chatId, new Screen(text, ShowcaseScreens.finishKeyboard(category)));
    }

    private static String caption(Message message) {
        String caption = message.caption();
        if (caption == null || caption.isBlank()) {
            return null;
        }
        String trimmed = caption.trim();
        return trimmed.length() > MAX_CAPTION_LENGTH ? trimmed.substring(0, MAX_CAPTION_LENGTH) : trimmed;
    }

    /** The category this user is adding items to, or empty if they are not in that step. */
    private Optional<ShowcaseCategory> awaitedCategory(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.AWAITING_SHOWCASE_MEDIA)
                .map(ConversationState::getData)
                .map(data -> data.get(ShowcaseCallbackHandler.CATEGORY_KEY))
                .flatMap(ShowcaseCategory::fromKey);
    }
}
