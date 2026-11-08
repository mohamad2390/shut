package com.shut.telegram.membership;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import com.shut.telegram.handler.BotMessages;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;
import com.shut.telegram.model.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lets only channel members through. Any handler that must be members-only calls
 * {@link #admit(long, long)} first and stops when it returns false.
 */
@Component
@TelegramBotEnabled
public class MembershipGate {

    private static final Logger log = LoggerFactory.getLogger(MembershipGate.class);

    private final ChannelMembershipService membership;
    private final TelegramApiClient telegram;
    private final String channelLink;

    public MembershipGate(ChannelMembershipService membership, TelegramApiClient telegram,
                          TelegramProperties properties) {
        this.membership = membership;
        this.telegram = telegram;
        this.channelLink = properties.channelLink();
    }

    /** Returns true if the user may continue; otherwise tells them why and returns false. */
    public boolean admit(long userId, long chatId) {
        try {
            if (membership.isMember(userId)) {
                return true;
            }
            telegram.sendMessage(joinPrompt(chatId));
        } catch (TelegramApiException e) {
            log.error("Membership check failed: {}", e.getMessage());
            telegram.sendMessage(SendMessageRequest.text(chatId, BotMessages.TEMPORARY_ERROR));
        }
        return false;
    }

    private SendMessageRequest joinPrompt(long chatId) {
        SendMessageRequest request = SendMessageRequest.text(chatId, BotMessages.JOIN_REQUIRED);
        if (channelLink == null || channelLink.isBlank()) {
            return request;
        }
        return request.withReplyMarkup(InlineKeyboardMarkup.singleButton(
                InlineKeyboardButton.link(BotMessages.JOIN_BUTTON, channelLink)));
    }
}
