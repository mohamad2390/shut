package com.shut.telegram.dispatch;

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

    public UpdateDispatcher(List<UpdateHandler> handlers) {
        this.handlers = handlers;
    }

    public void dispatch(Update update) {
        handlers.stream()
                .filter(handler -> handler.supports(update))
                .findFirst()
                .ifPresentOrElse(
                        handler -> handler.handle(update),
                        () -> log.debug("No handler for update {}", update.updateId()));
    }
}
