package com.shut.telegram.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.browse.ProviderDetails;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.review.RatingSummary;
import com.shut.review.ReviewService;
import com.shut.review.ReviewView;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewsWidgetTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ProviderDetails ALI = new ProviderDetails(new ProfessionalProfileResponse(
            1L, 7L, "علی", null, null, 0, null, null, null, null, null, Set.of(), Set.of(), Set.of(), null, null), "ali");

    private static ReviewView review(String name, int stars, String comment) {
        return new ReviewView(name, stars, comment, OffsetDateTime.now());
    }

    @Test
    void eachRowHasReviewerStarsAndComment() throws Exception {
        String state = ReviewsWidgetState.page("علی", new RatingSummary(4.5, 2),
                List.of(review("سارا", 5, "عالی بود"), review("رضا", 4, null)), 3, 2);

        JsonNode json = JSON.readTree(state);
        assertThat(json.get("i").toString()).isEqualTo("[[\"سارا\",5,\"عالی بود\"],[\"رضا\",4,\"\"]]");
        assertThat(json.get("r").asInt()).isEqualTo(45);
        assertThat(json.get("n").asInt()).isEqualTo(2);
        assertThat(json.get("c").asInt()).isEqualTo(3);
        assertThat(json.get("p").asInt()).isEqualTo(2);
    }

    @Test
    void aPageOfLongCommentsFitsInTelegramsLimit() {
        List<ReviewView> reviews = List.of(review("ن".repeat(60), 5, "م".repeat(500)),
                review("ن".repeat(60), 4, "م".repeat(500)), review("ن".repeat(60), 3, "م".repeat(500)));

        String state = ReviewsWidgetState.page("ک".repeat(80), new RatingSummary(4, 999), reviews, 99, 99);

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void aPageOfHalfSpacesCutsTheCommentsHarderInsteadOfBreakingTheLimit() {
        List<ReviewView> reviews = List.of(review("\u200c".repeat(14), 5, "\u200c".repeat(500)),
                review("\u200c".repeat(14), 4, "\u200c".repeat(500)), review("\u200c".repeat(14), 3, "\u200c".repeat(500)));

        String state = ReviewsWidgetState.page("\u200c".repeat(30), new RatingSummary(4, 999), reviews, 99, 99);

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void theAnswererPagesNewestFirstThreeAtATime() {
        ReviewService reviews = mock(ReviewService.class);
        when(reviews.summary(7L)).thenReturn(new RatingSummary(4, 5));
        when(reviews.page(7L, 1, 3)).thenReturn(new PageImpl<>(List.of(review("رضا", 4, null)), PageRequest.of(1, 3), 4));

        String state = new ReviewsWidgetAnswerer(reviews).answer(ALI, "p:2");

        assertThat(state).contains("\"p\":2").contains("\"c\":2").contains("رضا");
    }

    @Test
    void aPageBeyondTheEndBecomesTheLastPage() {
        ReviewService reviews = mock(ReviewService.class);
        when(reviews.summary(7L)).thenReturn(new RatingSummary(4, 4));
        when(reviews.page(7L, 9, 3)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(9, 3), 4));
        when(reviews.page(7L, 1, 3)).thenReturn(new PageImpl<>(List.of(review("رضا", 4, null)), PageRequest.of(1, 3), 4));

        assertThat(new ReviewsWidgetAnswerer(reviews).answer(ALI, "p:10")).contains("\"p\":2").contains("رضا");
    }

    @Test
    void noReviewsStillGivesOneEmptyPage() throws Exception {
        ReviewService reviews = mock(ReviewService.class);
        when(reviews.summary(7L)).thenReturn(RatingSummary.NONE);
        when(reviews.page(7L, 0, 3)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));

        JsonNode json = JSON.readTree(new ReviewsWidgetAnswerer(reviews).answer(ALI, "i"));

        assertThat(json.get("i")).isEmpty();
        assertThat(json.get("c").asInt()).isEqualTo(1);
        assertThat(json.get("r").asInt()).isZero();
    }
}
