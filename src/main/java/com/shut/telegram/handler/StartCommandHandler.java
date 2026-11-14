package com.shut.telegram.handler;

import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.browse.ProviderPageOpener;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.menu.MainMenuScreen;
import com.shut.telegram.messaging.BotLinks;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

/** /start: creates the account on first contact and opens the main menu, or a member's page for a profile link. */
@Component
@TelegramBotEnabled
class StartCommandHandler extends CommandHandler {

    private final TelegramAccountResolver accounts;
    private final ConversationService conversations;
    private final BotMessenger messenger;
    private final ProviderPageOpener pages;

    StartCommandHandler(MembershipGate membershipGate, TelegramAccountResolver accounts,
                        ConversationService conversations, BotMessenger messenger,
                        ProviderPageOpener pages) {
        super(membershipGate);
        this.accounts = accounts;
        this.conversations = conversations;
        this.messenger = messenger;
        this.pages = pages;
    }

    @Override
    protected String command() {
        return "start";
    }

    @Override
    protected void onCommand(Message message, TelegramUser from) {
        accounts.resolve(from);
        conversations.clear(from.id());
        long chatId = message.chat().id();
        if (BotLinks.profileIdIn(message.text()).stream().anyMatch(id -> pages.open(chatId, id))) {
            return;
        }
        messenger.send(chatId, MainMenuScreen.build(BotMessages.welcome(from.firstName())));
    }
}
