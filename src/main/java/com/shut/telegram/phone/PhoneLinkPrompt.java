package com.shut.telegram.phone;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.model.SendReplyKeyboardRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Starts the phone linking: asks the member to share their number with the button under the text box. */
@Component
@TelegramBotEnabled
public class PhoneLinkPrompt {

    private final ConversationService conversations;
    private final TelegramApiClient telegram;

    PhoneLinkPrompt(ConversationService conversations, TelegramApiClient telegram) {
        this.conversations = conversations;
        this.telegram = telegram;
    }

    public void ask(long chatId, long telegramId) {
        conversations.begin(telegramId, ConversationStep.AWAITING_PHONE_CONTACT, Map.of());
        telegram.sendWithReplyKeyboard(SendReplyKeyboardRequest.askForContact(chatId, PhoneTexts.ASK, PhoneTexts.BUTTON));
    }
}
