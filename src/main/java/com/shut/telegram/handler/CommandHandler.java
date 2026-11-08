package com.shut.telegram.handler;

import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;

import java.util.Locale;

/**
 * Base class for slash-command handlers such as {@code /start}. It only lets channel members
 * in private chats through, so subclasses can focus on what the command does.
 */
public abstract class CommandHandler implements UpdateHandler {

    private final MembershipGate membershipGate;

    protected CommandHandler(MembershipGate membershipGate) {
        this.membershipGate = membershipGate;
    }

    /** The command name without the leading slash, in lower case. */
    protected abstract String command();

    protected abstract void onCommand(Message message, TelegramUser from);

    @Override
    public final boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.text() != null
                && command().equals(parseCommand(message.text()));
    }

    @Override
    public final void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        if (from == null || from.bot() || !message.chat().isPrivate()) {
            return;
        }
        if (!membershipGate.admit(from.id(), message.chat().id())) {
            return;
        }
        onCommand(message, from);
    }

    /** Extracts "start" from "/start", "/start payload" or "/start@MyBot payload". */
    private static String parseCommand(String text) {
        if (!text.startsWith("/")) {
            return "";
        }
        String token = text.substring(1).split("\\s+", 2)[0];
        int atSign = token.indexOf('@');
        String name = atSign >= 0 ? token.substring(0, atSign) : token;
        return name.toLowerCase(Locale.ROOT);
    }
}
