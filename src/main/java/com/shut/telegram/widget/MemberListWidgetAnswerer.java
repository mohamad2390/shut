package com.shut.telegram.widget;

import com.shut.browse.ProviderRankingService;
import com.shut.browse.ProviderSearchService;
import com.shut.browse.RankedProvider;
import com.shut.telegram.cleanup.ChatCleaner;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.handler.BotMessages;
import com.shut.telegram.menu.MainMenuScreen;
import com.shut.telegram.messaging.BotMessenger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** The members widget asks for a page of the ranking: {@code "i"} for the first, {@code "p:3"} for the third. */
@Component
@TelegramBotEnabled
class MemberListWidgetAnswerer implements SharedWidgetAnswerer {

    private static final String OPEN_PREFIX = "o:";
    /** What the back button of the widget asks for. */
    static final String HOME = "m";

    private final ProviderRankingService ranking;
    private final ProviderSearchService providers;
    private final ProviderMenuWidgetPresenter menus;
    private final ChatCleaner cleaner;
    private final ConversationService conversations;
    private final BotMessenger messenger;

    MemberListWidgetAnswerer(ProviderRankingService ranking, ProviderSearchService providers,
                             ProviderMenuWidgetPresenter menus, ChatCleaner cleaner, ConversationService conversations,
                             BotMessenger messenger) {
        this.cleaner = cleaner;
        this.conversations = conversations;
        this.messenger = messenger;
        this.ranking = ranking;
        this.providers = providers;
        this.menus = menus;
    }

    /** A tap on a member ({@code "o:<userId>"}) sends that member's four options as a new widget. */
    @Override
    public boolean open(long chatId, WidgetMessage message, String request) {
        if (HOME.equals(request)) {
            return true;
        }
        if (request == null || !request.startsWith(OPEN_PREFIX)) {
            return false;
        }
        try {
            return providers.find(Long.parseLong(request.substring(OPEN_PREFIX.length())))
                    .map(details -> menus.show(chatId, details) || true)
                    .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.MEMBERS;
    }

    /** The back button: once it is answered, the whole chat is cleaned up and the main menu opens. */
    @Override
    public void afterAnswer(long chatId, WidgetMessage message, String request) {
        if (HOME.equals(request)) {
            conversations.clear(chatId);
            cleaner.clear(chatId);
            messenger.send(chatId, MainMenuScreen.build(BotMessages.MENU_INTRO));
        }
    }

    @Override
    public String answer(WidgetMessage message, String request) {
        return state(message.getQuery(), PriceWidgetAnswerer.requestedPage(request)).orElseGet(
                () -> MemberListWidgetState.page(message.getQuery(), List.of(), 1, 1));
    }

    /** The first state of a widget for {@code query}, or empty if nobody matches. */
    Optional<String> firstState(String query) {
        return state(query, 1);
    }

    /** The state for a page counted from 1; a page past the end becomes the last one. Empty if nobody matches. */
    private Optional<String> state(String query, int page) {
        int index = Math.max(1, page) - 1;
        Page<RankedProvider> found = ranking.page(query, index, MemberListWidgetState.PAGE_SIZE);
        if (found.isEmpty() && index > 0 && found.getTotalPages() > 0) {
            found = ranking.page(query, found.getTotalPages() - 1, MemberListWidgetState.PAGE_SIZE);
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MemberListWidgetState.page(query, found.getContent(), found.getTotalPages(), found.getNumber() + 1));
    }
}
