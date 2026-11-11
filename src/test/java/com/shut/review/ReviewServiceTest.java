package com.shut.review;

import com.shut.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private static final Long PROVIDER = 1L;
    private static final Long REVIEWER = 2L;

    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final ReviewService service = new ReviewService(repository, mock(UserRepository.class));

    @Test
    void aMemberCannotRateThemselves() {
        assertThatThrownBy(() -> service.rate(PROVIDER, PROVIDER, 5)).isInstanceOf(SelfReviewException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void ratingsOutsideOneToFiveAreRefused() {
        assertThatThrownBy(() -> service.rate(PROVIDER, REVIEWER, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rate(PROVIDER, REVIEWER, 6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFirstRatingIsSaved() {
        when(repository.findByProviderIdAndReviewerId(PROVIDER, REVIEWER)).thenReturn(Optional.empty());
        when(repository.save(any(Review.class))).thenAnswer(call -> call.getArgument(0));

        Review review = service.rate(PROVIDER, REVIEWER, 4);

        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getProviderId()).isEqualTo(PROVIDER);
        assertThat(review.getReviewerId()).isEqualTo(REVIEWER);
    }

    @Test
    void ratingAgainChangesTheStarsAndKeepsTheComment() {
        Review existing = new Review(PROVIDER, REVIEWER, 3);
        existing.changeComment("خوب بود");
        when(repository.findByProviderIdAndReviewerId(PROVIDER, REVIEWER)).thenReturn(Optional.of(existing));

        service.rate(PROVIDER, REVIEWER, 5);

        assertThat(existing.getRating()).isEqualTo(5);
        assertThat(existing.getComment()).isEqualTo("خوب بود");
        verify(repository, never()).save(any());
    }

    @Test
    void aCommentNeedsARatingFirst() {
        when(repository.findByProviderIdAndReviewerId(PROVIDER, REVIEWER)).thenReturn(Optional.empty());

        assertThat(service.comment(PROVIDER, REVIEWER, "عالی")).isFalse();
    }

    @Test
    void aCommentIsTrimmedAndABlankOneRemovesIt() {
        Review existing = new Review(PROVIDER, REVIEWER, 3);
        when(repository.findByProviderIdAndReviewerId(PROVIDER, REVIEWER)).thenReturn(Optional.of(existing));

        service.comment(PROVIDER, REVIEWER, "  عالی بود \n");
        assertThat(existing.getComment()).isEqualTo("عالی بود");

        service.comment(PROVIDER, REVIEWER, "   ");
        assertThat(existing.getComment()).isNull();
    }

    @Test
    void aTooLongCommentIsRefused() {
        assertThatThrownBy(() -> service.comment(PROVIDER, REVIEWER, "x".repeat(ReviewService.MAX_COMMENT_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summaryIsEmptyWithoutRatings() {
        ReviewRepository.Aggregate none = aggregate(null, 0);
        when(repository.aggregate(PROVIDER)).thenReturn(none);

        assertThat(service.summary(PROVIDER).isEmpty()).isTrue();
    }

    @Test
    void summaryCarriesAverageAndCount() {
        when(repository.aggregate(PROVIDER)).thenReturn(aggregate(4.5, 12));

        RatingSummary summary = service.summary(PROVIDER);

        assertThat(summary.average()).isEqualTo(4.5);
        assertThat(summary.count()).isEqualTo(12);
    }

    @Test
    void deletingSomeoneElsesReviewDoesNothing() {
        when(repository.findByProviderIdAndReviewerId(PROVIDER, REVIEWER)).thenReturn(Optional.empty());

        assertThat(service.deleteOwn(PROVIDER, REVIEWER)).isFalse();
        verify(repository, never()).delete(any());
    }

    private static ReviewRepository.Aggregate aggregate(Double average, long total) {
        return new ReviewRepository.Aggregate() {
            @Override
            public Double getAverage() {
                return average;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
