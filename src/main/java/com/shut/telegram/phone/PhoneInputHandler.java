package com.shut.telegram.phone;

import com.shut.auth.AccountLinkService;
import com.shut.auth.InvalidPhoneException;
import com.shut.auth.PhoneNumbers;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.menu.MyInfoScreen;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Contact;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.SendReplyKeyboardRequest;
import com.shut.telegram.model.Update;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Receives the contact the member shared and ties its phone number to their account. Only a contact that belongs to
 * the sender is accepted, so nobody can claim a number that is not theirs.
 */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class PhoneInputHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final AccountLinkService links;
    private final TelegramApiClient telegram;
    private final BotMessenger messenger;

    PhoneInputHandler(ConversationService conversations, MembershipGate membershipGate,
                      TelegramAccountResolver accounts, AccountLinkService links, TelegramApiClient telegram,
                      BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.links = links;
        this.telegram = telegram;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && (message.contact() != null || (message.text() != null && !message.text().startsWith("/")))
                && conversations.find(message.from().id())
                .filter(state -> state.getStep() == ConversationStep.AWAITING_PHONE_CONTACT)
                .isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        long chatId = message.chat().id();
        long telegramId = message.from().id();
        if (!membershipGate.admit(telegramId, chatId)) {
            return;
        }
        Contact contact = message.contact();
        if (contact == null) {
            telegram.sendWithReplyKeyboard(SendReplyKeyboardRequest.askForContact(
                    chatId, PhoneTexts.USE_THE_BUTTON, PhoneTexts.BUTTON));
            return;
        }
        if (contact.userId() == null || contact.userId() != telegramId) {
            telegram.sendWithReplyKeyboard(SendReplyKeyboardRequest.askForContact(
                    chatId, PhoneTexts.NOT_YOURS, PhoneTexts.BUTTON));
            return;
        }
        String text;
        try {
            String phone = PhoneNumbers.normalize(contact.phoneNumber());
            text = switch (links.linkPhone(accounts.resolve(message.from()).getId(), phone)) {
                case LINKED -> PhoneTexts.LINKED;
                case MERGED -> PhoneTexts.MERGED;
                case ALREADY_LINKED -> PhoneTexts.ALREADY;
                case TAKEN -> PhoneTexts.TAKEN;
            };
        } catch (InvalidPhoneException e) {
            text = PhoneTexts.NOT_IRANIAN;
        }
        conversations.clear(telegramId);
        telegram.sendWithReplyKeyboard(SendReplyKeyboardRequest.removeKeyboard(chatId, text));
        messenger.send(chatId, accounts.resolve(message.from()).isProvider()
                ? MyInfoScreen.forSpecialist() : MyInfoScreen.forCustomer());
    }
}
