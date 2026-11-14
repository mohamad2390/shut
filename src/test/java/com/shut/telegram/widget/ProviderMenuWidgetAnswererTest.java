package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingService;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.review.RatingSummary;
import com.shut.review.ReviewService;
import com.shut.telegram.messaging.BotMessenger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderMenuWidgetAnswererTest {

    private static final long CHAT = 500L;

    private final ReviewService reviews = mock(ReviewService.class);
    private final PricingService pricing = mock(PricingService.class);
    private final ProfileWidgetPresenter profiles = mock(ProfileWidgetPresenter.class);
    private final CalendarWidgetPresenter calendars = mock(CalendarWidgetPresenter.class);
    private final PriceWidgetPresenter prices = mock(PriceWidgetPresenter.class);
    private final ShowcaseWidgetPresenter showcases = mock(ShowcaseWidgetPresenter.class);
    private final ReviewsWidgetPresenter reviewWidgets = mock(ReviewsWidgetPresenter.class);
    private final BotMessenger messenger = mock(BotMessenger.class);
    private final ProviderMenuWidgetAnswerer answerer = new ProviderMenuWidgetAnswerer(
            reviews, pricing, profiles, calendars, prices, showcases, reviewWidgets, messenger);
    private final ProviderDetails ali = new ProviderDetails(new ProfessionalProfileResponse(
            1L, 7L, "علی", null, null, 0, null, null, null, null, null, Set.of(), Set.of(), Set.of(), null, null), "ali");

    @Test
    void profileOpensTheProfileWidget() {
        when(profiles.show(CHAT, ali)).thenReturn(true);

        assertThat(answerer.open(CHAT, ali, "o:profile")).isTrue();

        verify(profiles).show(CHAT, ali);
        verify(messenger, never()).sendText(anyLong(), anyString());
    }

    @Test
    void calendarOpensTheCalendarWidgetForThatMember() {
        when(calendars.show(CHAT, 7L, "علی")).thenReturn(true);

        assertThat(answerer.open(CHAT, ali, "o:calendar")).isTrue();

        verify(calendars).show(CHAT, 7L, "علی");
    }

    @Test
    void pricesOpenThePriceWidgetWithTheirItemsEvenWhenThereAreNone() {
        when(pricing.list(7L)).thenReturn(List.<PriceItem>of());
        when(prices.show(eq(CHAT), eq(7L), eq("علی"), anyList())).thenReturn(true);

        assertThat(answerer.open(CHAT, ali, "o:prices")).isTrue();

        verify(prices).show(CHAT, 7L, "علی", List.of());
    }

    @Test
    void showcaseOpensTheShowcaseWidget() {
        when(showcases.show(CHAT, ali)).thenReturn(true);

        assertThat(answerer.open(CHAT, ali, "o:showcase")).isTrue();

        verify(showcases).show(CHAT, ali);
    }

    @Test
    void reviewsOpenTheReviewsWidget() {
        when(reviewWidgets.show(CHAT, ali)).thenReturn(true);

        assertThat(answerer.open(CHAT, ali, "o:reviews")).isTrue();

        verify(reviewWidgets).show(CHAT, ali);
    }

    @Test
    void aWidgetThatCannotBeSentIsExplainedInTheChat() {
        when(profiles.show(CHAT, ali)).thenReturn(false);

        assertThat(answerer.open(CHAT, ali, "o:profile")).isTrue();

        verify(messenger).sendText(CHAT, ProviderMenuWidgetAnswerer.FAILED);
    }

    @Test
    void unknownRequestsAreNotOpened() {
        assertThat(answerer.open(CHAT, ali, "p:2")).isFalse();
        assertThat(answerer.open(CHAT, ali, null)).isFalse();
        verify(profiles, never()).show(anyLong(), any());
    }

    @Test
    void theStateHasTheNameAndTheRating() {
        when(reviews.summary(7L)).thenReturn(new RatingSummary(4.5, 3));

        assertThat(answerer.answer(ali, "i")).isEqualTo("{\"h\":\"علی\",\"r\":45,\"n\":3}");
    }
}
