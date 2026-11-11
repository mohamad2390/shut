package com.shut.telegram.browse;

import com.shut.browse.ProviderSearchService;
import com.shut.browse.ProviderSummary;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runs a search for a user and remembers what they searched for, so that paging through the results
 * and coming back from a member's page lands where they left off.
 */
@Component
@TelegramBotEnabled
class BrowseSession {

    static final int PAGE_SIZE = 8;
    private static final String QUERY_KEY = "query";
    private static final String PAGE_KEY = "page";

    private final ProviderSearchService search;
    private final TelegramAccountResolver accounts;
    private final ConversationService conversations;

    BrowseSession(ProviderSearchService search, TelegramAccountResolver accounts, ConversationService conversations) {
        this.search = search;
        this.accounts = accounts;
        this.conversations = conversations;
    }

    /** Runs the search, remembers it and returns the page of results. */
    Screen results(TelegramUser from, String query, int page) {
        Long userId = accounts.resolve(from).getId();
        Page<ProviderSummary> found = search.search(query, userId, page, PAGE_SIZE);
        // The page asked for can be gone if members left since; show the last one that exists.
        if (found.isEmpty() && page > 0 && found.getTotalElements() > 0) {
            return results(from, query, (int) ((found.getTotalElements() - 1) / PAGE_SIZE));
        }
        conversations.begin(from.id(), ConversationStep.BROWSING_PROVIDERS,
                Map.of(QUERY_KEY, query, PAGE_KEY, Integer.toString(found.getNumber())));
        return BrowseScreens.results(query, found);
    }

    /** The results the user was last looking at, or everyone if that was forgotten. */
    Screen lastResults(TelegramUser from, Integer pageOverride) {
        Optional<Map<String, String>> saved = browsing(from.id());
        String query = saved.map(data -> data.getOrDefault(QUERY_KEY, "")).orElse("");
        int page = pageOverride != null ? pageOverride : saved.map(data -> parsePage(data.get(PAGE_KEY))).orElse(0);
        return results(from, query, page);
    }

    /** {@code data} plus the user's current search, for a step that should hand the search back when it ends. */
    Map<String, String> withSearch(long telegramId, Map<String, String> data) {
        Map<String, String> combined = new HashMap<>(data);
        browsing(telegramId).ifPresent(saved -> {
            copy(saved, combined, QUERY_KEY);
            copy(saved, combined, PAGE_KEY);
        });
        return combined;
    }

    /** Ends whatever step the user is on. If it carried a search, they are back in that search. */
    void resume(long telegramId) {
        Map<String, String> data = conversations.find(telegramId).map(ConversationState::getData).orElse(Map.of());
        if (!data.containsKey(QUERY_KEY)) {
            conversations.clear(telegramId);
            return;
        }
        Map<String, String> search = new HashMap<>();
        copy(data, search, QUERY_KEY);
        copy(data, search, PAGE_KEY);
        conversations.begin(telegramId, ConversationStep.BROWSING_PROVIDERS, search);
    }

    private static void copy(Map<String, String> from, Map<String, String> to, String key) {
        if (from.containsKey(key)) {
            to.put(key, from.get(key));
        }
    }

    private Optional<Map<String, String>> browsing(long telegramId) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == ConversationStep.BROWSING_PROVIDERS)
                .map(ConversationState::getData);
    }

    private static int parsePage(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
