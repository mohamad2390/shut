package com.shut.telegram.browse;

import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseItem;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.SendMediaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** Sends a page of a member's showcase to a chat: the files, then a message with the way to the next page. */
@Component
@TelegramBotEnabled
public class ShowcaseSender {

    static final int PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(ShowcaseSender.class);

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final ShowcaseService showcase;

    ShowcaseSender(TelegramApiClient telegram, BotMessenger messenger, ShowcaseService showcase) {
        this.telegram = telegram;
        this.messenger = messenger;
        this.showcase = showcase;
    }

    /** Returns false if the page has nothing in it, and nothing was sent. */
    public boolean sendPage(long chatId, long userId, ShowcaseCategory category, int page) {
        Page<ShowcaseItem> items = showcase.page(userId, category, page, PAGE_SIZE);
        if (items.isEmpty()) {
            return false;
        }
        items.forEach(item -> sendItem(chatId, item));
        messenger.send(chatId, BrowseScreens.showcasePage(userId, category, page, PAGE_SIZE, items.getTotalElements()));
        return true;
    }

    /** One broken file must not stop the rest of the page from being sent. */
    private void sendItem(long chatId, ShowcaseItem item) {
        try {
            telegram.sendMedia(switch (item.getMediaType()) {
                case PHOTO -> SendMediaRequest.photo(chatId, item.getFileId(), item.getCaption(), null);
                case VIDEO -> SendMediaRequest.video(chatId, item.getFileId(), item.getCaption(), null);
                case DOCUMENT -> SendMediaRequest.document(chatId, item.getFileId(), item.getCaption(), null);
            });
        } catch (TelegramApiException e) {
            log.warn("Could not send showcase item {}: {}", item.getId(), e.getMessage());
        }
    }
}
