package com.shut.telegram.handler;

import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.menu.MainMenuScreen;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** /cancel: abandons the current step and returns to the main menu. */
@Component
@TelegramBotEnabled
class CancelCommandHandler extends CommandHandler {

    private final ConversationService conversations;
    private final BotMessenger messenger;

    CancelCommandHandler(MembershipGate membershipGate, ConversationService conversations, BotMessenger messenger) {
        super(membershipGate);
        this.conversations = conversations;
        this.messenger = messenger;
    }

    @Override
    protected String command() {
        return "cancel";
    }

    @Override
    protected void onCommand(Message message, TelegramUser from) {
        conversations.clear(from.id());
        messenger.send(message.chat().id(), MainMenuScreen.build(BotMessages.CANCELLED));
    }
}
