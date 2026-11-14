package com.shut.telegram.widget;

import com.shut.browse.ProviderRankingService;
import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.browse.RankedProvider;
import com.shut.telegram.cleanup.ChatCleaner;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.menu.MainMenuScreen;
import com.shut.telegram.messaging.BotMessenger;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberListWidgetAnswererTest {

    private final ProviderRankingService ranking = mock(ProviderRankingService.class);
    private final ProviderSearchService providers = mock(ProviderSearchService.class);
    private final ProviderMenuWidgetPresenter menus = mock(ProviderMenuWidgetPresenter.class);
    private final ChatCleaner cleaner = mock(ChatCleaner.class);
    private final ConversationService conversations = mock(ConversationService.class);
    private final BotMessenger messenger = mock(BotMessenger.class);
    private final MemberListWidgetAnswerer answerer = new MemberListWidgetAnswerer(
            ranking, providers, menus, cleaner, conversations, messenger);
    private final WidgetMessage everyone = new WidgetMessage(1L, 2L, null, WidgetKind.MEMBERS);

    private static PageImpl<RankedProvider> page(int index, int total, RankedProvider... members) {
        return new PageImpl<>(List.of(members), PageRequest.of(index, 5), total);
    }

    private static RankedProvider member(long id, String name) {
        return new RankedProvider(id, name, "قم", 4, 2, List.of());
    }

    @Test
    void firstRequestAsksForTheFirstPageOfFive() {
        when(ranking.page(null, 0, 5)).thenReturn(page(0, 7, member(1, "علی")));

        String state = answerer.answer(everyone, "i");

        verify(ranking).page(null, 0, 5);
        assertThat(state).contains("\"c\":2").contains("\"p\":1").contains("علی");
    }

    @Test
    void pageNIsCountedFromOne() {
        when(ranking.page(null, 1, 5)).thenReturn(page(1, 7, member(2, "رضا")));

        assertThat(answerer.answer(everyone, "p:2")).contains("\"p\":2").contains("رضا");
    }

    @Test
    void aPageBeyondTheEndFallsBackToTheLastPage() {
        when(ranking.page(null, 9, 5)).thenReturn(page(9, 7));
        when(ranking.page(null, 1, 5)).thenReturn(page(1, 7, member(2, "رضا")));

        assertThat(answerer.answer(everyone, "p:10")).contains("\"p\":2").contains("رضا");
    }

    @Test
    void aSearchWidgetKeepsAskingForItsOwnWords() {
        WidgetMessage search = new WidgetMessage(1L, 3L, null, WidgetKind.MEMBERS, "ترکیه");
        when(ranking.page("ترکیه", 1, 5)).thenReturn(page(1, 7, member(2, "رضا")));

        String state = answerer.answer(search, "p:2");

        assertThat(state).contains("رضا").contains("نتیجه‌ی «ترکیه»");
    }

    @Test
    void firstStateIsEmptyWhenNobodyMatches() {
        when(ranking.page("هیچی", 0, 5)).thenReturn(page(0, 0));

        assertThat(answerer.firstState("هیچی")).isEmpty();
    }

    @Test
    void kindIsMembers() {
        assertThat(answerer.kind()).isEqualTo(WidgetKind.MEMBERS);
    }

    @Test
    void tappingAMemberSendsTheirMenuWidget() {
        ProviderDetails details = new ProviderDetails(null, "ali");
        when(providers.find(12L)).thenReturn(java.util.Optional.of(details));

        assertThat(answerer.open(500L, everyone, "o:12")).isTrue();

        verify(menus).show(500L, details);
    }

    @Test
    void otherRequestsAndGoneMembersAreNotOpened() {
        when(providers.find(13L)).thenReturn(java.util.Optional.empty());

        assertThat(answerer.open(500L, everyone, "p:2")).isFalse();
        assertThat(answerer.open(500L, everyone, "o:abc")).isFalse();
        assertThat(answerer.open(500L, everyone, "o:13")).isFalse();
    }

    @Test
    void theBackButtonIsAnsweredFirstAndThenCleansTheChatAndOpensTheMainMenu() {
        assertThat(answerer.open(500L, everyone, "m")).isTrue();
        org.mockito.Mockito.verifyNoInteractions(cleaner, messenger);

        answerer.afterAnswer(500L, everyone, "m");

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(conversations, cleaner, messenger);
        order.verify(conversations).clear(500L);
        order.verify(cleaner).clear(500L);
        order.verify(messenger).send(org.mockito.ArgumentMatchers.eq(500L), org.mockito.ArgumentMatchers.any(
                com.shut.telegram.messaging.Screen.class));
    }

    @Test
    void otherRequestsDoNotCleanAnything() {
        answerer.afterAnswer(500L, everyone, "p:2");

        org.mockito.Mockito.verifyNoInteractions(cleaner, messenger, conversations);
        assertThat(MainMenuScreen.build("x")).isNotNull();
    }
}
