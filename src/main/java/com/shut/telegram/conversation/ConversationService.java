package com.shut.telegram.conversation;

import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/** Remembers which step of a multi-step flow each Telegram user is on. */
@Service
@TelegramBotEnabled
public class ConversationService {

    private final ConversationStateRepository repository;

    public ConversationService(ConversationStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<ConversationState> find(long telegramId) {
        return repository.findById(telegramId);
    }

    /** Starts a step for the user, replacing whatever step they were on. */
    @Transactional
    public void begin(long telegramId, ConversationStep step, Map<String, String> data) {
        ConversationState state = repository.findById(telegramId)
                .orElseGet(() -> new ConversationState(telegramId));
        state.restart(step, data);
        repository.save(state);
    }

    @Transactional
    public void clear(long telegramId) {
        repository.deleteById(telegramId);
    }
}
