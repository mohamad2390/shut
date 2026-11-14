package com.shut.telegram.dispatch;

import com.shut.telegram.cleanup.ChatMessageLog;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Routes each update to the first handler that supports it. */
@Component
@TelegramBotEnabled
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final List<UpdateHandler> handlers;
    private final ChatMessageLog messageLog;

    public UpdateDispatcher(List<UpdateHandler> handlers, ChatMessageLog messageLog) {
        this.handlers = handlers;
        this.messageLog = messageLog;
    }

    public void dispatch(Update update) {
        rememberMessage(update);
        handlers.stream()
                .filter(handler -> handler.supports(update))
                .findFirst()
                .ifPresentOrElse(
                        handler -> handler.handle(update),
                        () -> log.debug("No handler for update {}", update.updateId()));
    }

    /** What a user sends in a private chat is remembered too, so that cleaning the chat removes it as well. */
    private void rememberMessage(Update update) {
        if (update.message() != null && update.message().chat() != null && update.message().chat().isPrivate()) {
            messageLog.record(update.message().chat().id(), update.message().messageId());
        }
    }
}
