package com.shut.telegram.profile;

import com.shut.profile.dto.ProfessionalProfileRequest;
import com.shut.profile.service.ProfessionalProfileService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Receives the text a user types after choosing a profile field to edit, and saves it. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class ProfileInputHandler implements UpdateHandler {

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final ProfessionalProfileService profiles;
    private final ProfileScreenProvider profileScreens;
    private final BotMessenger messenger;

    ProfileInputHandler(ConversationService conversations, MembershipGate membershipGate,
                        TelegramAccountResolver accounts, ProfessionalProfileService profiles,
                        ProfileScreenProvider profileScreens, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.profiles = profiles;
        this.profileScreens = profileScreens;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && message.text() != null
                && !message.text().startsWith("/")
                && awaitedField(message.from().id()).isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();

        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        ProfileField field = awaitedField(from.id()).orElse(null);
        if (field == null) {
            return;
        }

        try {
            ProfessionalProfileRequest request = field.toRequest(message.text());
            profiles.saveOrUpdate(accounts.resolve(from).getId(), request);
        } catch (InvalidProfileInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }

        conversations.clear(from.id());
        messenger.sendText(chatId, "✅ " + field.label() + " ذخیره شد.");
        messenger.send(chatId, profileScreens.forUser(from));
    }

    /** The profile field this user was asked for, or empty if they are not in that step. */
    private Optional<ProfileField> awaitedField(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.AWAITING_PROFILE_FIELD)
                .map(ConversationState::getData)
                .map(data -> data.get(ProfileCallbackHandler.FIELD_KEY))
                .flatMap(ProfileField::fromKey);
    }
}
