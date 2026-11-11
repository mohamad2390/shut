package com.shut.telegram.browse;

import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.Update;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Receives the text a user types after opening the search, and shows the matching members. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class SearchInputHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final BrowseSession session;
    private final BotMessenger messenger;

    SearchInputHandler(ConversationService conversations, MembershipGate membershipGate,
                       BrowseSession session, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.session = session;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && message.text() != null
                && !message.text().startsWith("/")
                && conversations.find(message.from().id())
                .filter(state -> state.getStep() == ConversationStep.AWAITING_SEARCH_QUERY)
                .isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        long chatId = message.chat().id();
        if (!membershipGate.admit(message.from().id(), chatId)) {
            return;
        }
        messenger.send(chatId, session.results(message.from(), message.text(), 0));
    }
}
