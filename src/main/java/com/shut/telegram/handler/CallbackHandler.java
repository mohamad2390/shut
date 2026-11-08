package com.shut.telegram.handler;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.model.CallbackQuery;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for inline-button handlers. Callback data looks like {@code prefix:arg1:arg2};
 * each subclass owns one prefix. Like commands, buttons are for channel members only.
 */
public abstract class CallbackHandler implements UpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackHandler.class);

    private final TelegramApiClient telegram;
    private final MembershipGate membershipGate;

    protected CallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate) {
        this.telegram = telegram;
        this.membershipGate = membershipGate;
    }

    /** The first part of the callback data this handler owns, without the colon. */
    protected abstract String prefix();

    protected abstract void onCallback(CallbackContext context);

    @Override
    public final boolean supports(Update update) {
        CallbackQuery query = update.callbackQuery();
        return query != null && query.data() != null && query.data().startsWith(prefix() + ":");
    }

    @Override
    public final void handle(Update update) {
        CallbackQuery query = update.callbackQuery();
        acknowledge(query);

        Message message = query.message();
        if (message == null) {
            return;
        }
        if (!membershipGate.admit(query.from().id(), message.chat().id())) {
            return;
        }

        List<String> args = List.of(query.data().substring(prefix().length() + 1).split(":"));
        onCallback(new CallbackContext(query.from(), message.chat().id(), message.messageId(), args));
    }

    /** Stops the spinner on the pressed button. A failure here must not block the action itself. */
    private void acknowledge(CallbackQuery query) {
        try {
            telegram.answerCallbackQuery(query.id());
        } catch (TelegramApiException e) {
            log.debug("Could not answer callback query: {}", e.getMessage());
        }
    }
}
