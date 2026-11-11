package com.shut.telegram.project;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Receives the text a user types while defining a project or writing an offer. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class ProjectInputHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final ProjectCreation creation;
    private final BidSubmission submission;
    private final BotMessenger messenger;

    ProjectInputHandler(ConversationService conversations, MembershipGate membershipGate, ProjectCreation creation,
                        BidSubmission submission, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.creation = creation;
        this.submission = submission;
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
                        .map(ConversationState::getStep)
                        .filter(step -> ProjectCreation.STEPS.contains(step) || BidSubmission.STEPS.contains(step))
                        .isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        ConversationStep step = conversations.find(from.id()).map(ConversationState::getStep).orElse(null);
        if (step == null) {
            return;
        }

        try {
            Optional<Screen> next = Optional.ofNullable(switch (step) {
                case AWAITING_PROJECT_TITLE -> creation.receiveTitle(from.id(), message.text());
                case AWAITING_PROJECT_DESCRIPTION -> creation.receiveDescription(from.id(), message.text());
                case AWAITING_PROJECT_CITY -> creation.receiveCity(from.id(), message.text());
                case AWAITING_PROJECT_BUDGET -> creation.receiveBudget(from.id(), message.text());
                case AWAITING_BID_AMOUNT -> submission.receiveAmount(from.id(), message.text());
                case AWAITING_BID_NOTE -> submission.submit(from, message.text());
                // These steps are answered with buttons; show the question again.
                default -> creation.currentPrompt(from.id()).orElse(null);
            });
            next.ifPresent(screen -> messenger.send(chatId, screen));
            if (step == ConversationStep.AWAITING_BID_NOTE) {
                messenger.sendText(chatId, "✅ پیشنهادت ثبت شد.");
            }
        } catch (InvalidProjectInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
        }
    }
}
