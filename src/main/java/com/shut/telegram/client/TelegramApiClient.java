package com.shut.telegram.client;

import com.shut.telegram.model.ChatMember;
import com.shut.telegram.model.EditMessageReplyMarkupRequest;
import com.shut.telegram.model.EditMessageTextRequest;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.telegram.model.SendMessageRequest;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;

import java.util.List;

/** The Bot API operations this application needs. */
public interface TelegramApiClient {

    /** Returns the bot's own identity; also validates the token. */
    TelegramUser getMe();

    /** Long-polls for new updates starting at {@code offset}. */
    List<Update> getUpdates(long offset, int timeoutSeconds);

    /** Looks up a user's status in a chat. The bot must be an administrator of that chat. */
    ChatMember getChatMember(String chatId, long userId);

    void sendMessage(SendMessageRequest request);

    /** Replaces the text and keyboard of a message the bot sent earlier. */
    void editMessageText(EditMessageTextRequest request);

    /** Sends a photo, video or document that Telegram already has, identified by its file id. */
    void sendMedia(SendMediaRequest request);

    /** Replaces only the keyboard of a message the bot sent earlier. */
    void editMessageReplyMarkup(EditMessageReplyMarkupRequest request);

    /** Copies a message into another chat and returns the id of the new message there. */
    long copyMessage(String toChatId, long fromChatId, long messageId);

    void deleteMessage(String chatId, long messageId);

    /** Stops the loading spinner on the button the user pressed. */
    void answerCallbackQuery(String callbackQueryId);

    /** Polling and webhooks are mutually exclusive, so any webhook must be removed first. */
    void deleteWebhook();
}
