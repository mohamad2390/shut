package com.shut.telegram.polling;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import com.shut.telegram.dispatch.UpdateDispatcher;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Long-polls Telegram on a background thread for as long as the application runs. */
@Component
@TelegramBotEnabled
public class UpdatePoller implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(UpdatePoller.class);
    private static final Duration STOP_WAIT = Duration.ofSeconds(5);

    private final TelegramApiClient telegram;
    private final UpdateDispatcher dispatcher;
    private final TelegramProperties properties;

    private volatile boolean running;
    private Thread thread;
    private long offset;
    private boolean prepared;

    public UpdatePoller(TelegramApiClient telegram, UpdateDispatcher dispatcher, TelegramProperties properties) {
        this.telegram = telegram;
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = Thread.ofPlatform().name("telegram-poller").daemon().start(this::runLoop);
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (thread == null) {
            return;
        }
        thread.interrupt();
        try {
            thread.join(STOP_WAIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void runLoop() {
        while (running) {
            try {
                if (!prepared) {
                    prepare();
                }
                pollOnce();
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.warn("Telegram polling failed, retrying in {}s: {}", properties.retryDelaySeconds(), e.getMessage());
                waitBeforeRetry();
            }
        }
        log.info("Telegram poller stopped");
    }

    /** Verifies the token and clears any webhook, which would otherwise block getUpdates. */
    private void prepare() {
        TelegramUser me = telegram.getMe();
        telegram.deleteWebhook();
        prepared = true;
        log.info("Telegram bot @{} connected, polling for updates", me.username());
    }

    private void pollOnce() {
        List<Update> updates = telegram.getUpdates(offset, properties.pollTimeoutSeconds());
        for (Update update : updates) {
            offset = update.updateId() + 1;
            try {
                dispatcher.dispatch(update);
            } catch (Exception e) {
                // One bad update must never stall the queue.
                log.error("Failed to handle update {}", update.updateId(), e);
            }
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(Duration.ofSeconds(properties.retryDelaySeconds()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
