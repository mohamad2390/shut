package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseItem;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.EditMessageReplyMarkupRequest;
import com.shut.telegram.model.InlineKeyboardMarkup;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.telegram.storage.MediaBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Handles every button in the showcase section. */
@Component
@TelegramBotEnabled
class ShowcaseCallbackHandler extends CallbackHandler {

    static final String CATEGORY_KEY = "category";
    private static final int PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(ShowcaseCallbackHandler.class);

    private final TelegramApiClient telegram;
    private final TelegramAccountResolver accounts;
    private final ShowcaseService showcase;
    private final ConversationService conversations;
    private final BotMessenger messenger;
    private final MediaBackupService backups;
    private final ShowcaseScreenProvider screens;

    ShowcaseCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                            TelegramAccountResolver accounts, ShowcaseService showcase,
                            ConversationService conversations, BotMessenger messenger,
                            MediaBackupService backups, ShowcaseScreenProvider screens) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.accounts = accounts;
        this.showcase = showcase;
        this.conversations = conversations;
        this.messenger = messenger;
        this.backups = backups;
        this.screens = screens;
    }

    @Override
    protected String prefix() {
        return ShowcaseCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> messenger.show(context.chatId(), context.messageId(), screens.home(context.user()));
            case "cat" -> withCategory(context, category -> showCategory(context, category));
            case "add" -> withCategory(context, category -> beginUpload(context, category));
            case "done" -> withCategory(context, category -> finishUpload(context, category));
            case "view" -> withCategory(context, category -> viewItems(context, category));
            case "del" -> askToConfirmDelete(context);
            case "delok" -> deleteItem(context);
            case "delno" -> restoreDeleteButton(context);
            default -> { }
        }
    }

    private void withCategory(CallbackContext context, Consumer<ShowcaseCategory> action) {
        ShowcaseCategory.fromKey(context.arg(1)).ifPresent(action);
    }

    private Long userId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void showCategory(CallbackContext context, ShowcaseCategory category) {
        long count = showcase.count(userId(context), category);
        messenger.show(context.chatId(), context.messageId(), ShowcaseScreens.category(category, count));
    }

    private void beginUpload(CallbackContext context, ShowcaseCategory category) {
        long remaining = ShowcaseService.MAX_ITEMS_PER_CATEGORY - showcase.count(userId(context), category);
        if (remaining <= 0) {
            messenger.sendText(context.chatId(), "این دسته پره. برای اضافه کردن، اول چندتا از نمونه‌کارها رو حذف کن.");
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_SHOWCASE_MEDIA,
                Map.of(CATEGORY_KEY, category.name()));
        messenger.show(context.chatId(), context.messageId(),
                ShowcaseScreens.uploadPrompt(category, remaining, backups.isEnabled()));
    }

    private void finishUpload(CallbackContext context, ShowcaseCategory category) {
        conversations.clear(context.user().id());
        showCategory(context, category);
    }

    private void viewItems(CallbackContext context, ShowcaseCategory category) {
        int page = parsePage(context.arg(2));
        Page<ShowcaseItem> items = showcase.page(userId(context), category, page, PAGE_SIZE);
        if (items.isEmpty()) {
            showCategory(context, category);
            return;
        }
        items.forEach(item -> telegram.sendMedia(toRequest(context.chatId(), item)));
        messenger.send(context.chatId(),
                ShowcaseScreens.pageNavigation(category, page, PAGE_SIZE, items.getTotalElements()));
    }

    private static SendMediaRequest toRequest(long chatId, ShowcaseItem item) {
        InlineKeyboardMarkup keyboard = ShowcaseScreens.deleteKeyboard(item.getId());
        return switch (item.getMediaType()) {
            case PHOTO -> SendMediaRequest.photo(chatId, item.getFileId(), item.getCaption(), keyboard);
            case VIDEO -> SendMediaRequest.video(chatId, item.getFileId(), item.getCaption(), keyboard);
            case DOCUMENT -> SendMediaRequest.document(chatId, item.getFileId(), item.getCaption(), keyboard);
        };
    }

    private static int parsePage(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void askToConfirmDelete(CallbackContext context) {
        parseItemId(context).ifPresent(itemId -> replaceKeyboard(context, ShowcaseScreens.confirmDeleteKeyboard(itemId)));
    }

    private void restoreDeleteButton(CallbackContext context) {
        parseItemId(context).ifPresent(itemId -> replaceKeyboard(context, ShowcaseScreens.deleteKeyboard(itemId)));
    }

    private void deleteItem(CallbackContext context) {
        parseItemId(context).ifPresent(itemId -> {
            showcase.deleteOwned(itemId, userId(context)).ifPresent(item -> backups.remove(item.getBackupMessageId()));
            removeMessage(context);
        });
    }

    private void replaceKeyboard(CallbackContext context, InlineKeyboardMarkup keyboard) {
        try {
            telegram.editMessageReplyMarkup(
                    new EditMessageReplyMarkupRequest(context.chatId(), context.messageId(), keyboard));
        } catch (TelegramApiException e) {
            log.debug("Could not change keyboard: {}", e.getMessage());
        }
    }

    private void removeMessage(CallbackContext context) {
        try {
            telegram.deleteMessage(Long.toString(context.chatId()), context.messageId());
        } catch (TelegramApiException e) {
            log.debug("Could not delete message: {}", e.getMessage());
        }
    }

    private static Optional<Long> parseItemId(CallbackContext context) {
        try {
            return Optional.of(Long.parseLong(context.arg(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
