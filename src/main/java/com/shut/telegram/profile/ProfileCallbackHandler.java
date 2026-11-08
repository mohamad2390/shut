package com.shut.telegram.profile;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Handles the profile page buttons: start editing a field, or back out of editing. */
@Component
@TelegramBotEnabled
class ProfileCallbackHandler extends CallbackHandler {

    static final String FIELD_KEY = "field";

    private final ConversationService conversations;
    private final BotMessenger messenger;
    private final ProfileScreenProvider profileScreens;

    ProfileCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                           ConversationService conversations, BotMessenger messenger,
                           ProfileScreenProvider profileScreens) {
        super(telegram, membershipGate);
        this.conversations = conversations;
        this.messenger = messenger;
        this.profileScreens = profileScreens;
    }

    @Override
    protected String prefix() {
        return ProfileCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "edit" -> startEditing(context);
            case "cancel" -> cancelEditing(context);
            default -> { }
        }
    }

    private void startEditing(CallbackContext context) {
        ProfileField.fromKey(context.arg(1)).ifPresent(field -> {
            conversations.begin(context.user().id(), ConversationStep.AWAITING_PROFILE_FIELD,
                    Map.of(FIELD_KEY, field.name()));
            Screen prompt = new Screen(
                    field.prompt(),
                    new KeyboardBuilder()
                            .row(InlineKeyboardButton.callback("انصراف", ProfileCallbacks.CANCEL))
                            .build());
            messenger.show(context.chatId(), context.messageId(), prompt);
        });
    }

    private void cancelEditing(CallbackContext context) {
        conversations.clear(context.user().id());
        messenger.show(context.chatId(), context.messageId(), profileScreens.forUser(context.user()));
    }
}
