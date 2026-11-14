package com.shut.telegram.handler;

import com.shut.telegram.cleanup.ChatCleaner;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.menu.MainMenuScreen;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** /clear: deletes everything in the chat that the bot knows about, then opens the main menu. */
@Component
@TelegramBotEnabled
class ClearCommandHandler extends CommandHandler {

    private final ConversationService conversations;
    private final ChatCleaner cleaner;
    private final BotMessenger messenger;

    ClearCommandHandler(MembershipGate membershipGate, ConversationService conversations, ChatCleaner cleaner,
                        BotMessenger messenger) {
        super(membershipGate);
        this.conversations = conversations;
        this.cleaner = cleaner;
        this.messenger = messenger;
    }

    @Override
    protected String command() {
        return "clear";
    }

    @Override
    protected void onCommand(Message message, TelegramUser from) {
        conversations.clear(from.id());
        cleaner.clear(message.chat().id());
        messenger.send(message.chat().id(), MainMenuScreen.build(BotMessages.MENU_INTRO));
    }
}
