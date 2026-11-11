package com.shut.review;

import com.shut.user.entity.User;
import com.shut.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ratings and comments members leave about each other. One per reviewer and provider; the reviewer can change or remove it. */
@Service
public class ReviewService {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_COMMENT_LENGTH = 500;

    private static final String ANONYMOUS = "یک عضو";

    private final ReviewRepository reviews;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, UserRepository users) {
        this.reviews = reviews;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(Long providerId) {
        ReviewRepository.Aggregate aggregate = reviews.aggregate(providerId);
        if (aggregate == null || aggregate.getTotal() == 0 || aggregate.getAverage() == null) {
            return RatingSummary.NONE;
        }
        return new RatingSummary(aggregate.getAverage(), aggregate.getTotal());
    }

    /** The rating and comment this reviewer gave, if any. */
    @Transactional(readOnly = true)
    public Optional<Review> findOwn(Long providerId, Long reviewerId) {
        return reviews.findByProviderIdAndReviewerId(providerId, reviewerId);
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public Page<ReviewView> page(Long providerId, int page, int size) {
        Page<Review> found = reviews.findByProviderId(providerId, PageRequest.of(Math.max(0, page), size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Map<Long, String> names = namesOf(found.getContent());
        return found.map(review -> new ReviewView(names.getOrDefault(review.getReviewerId(), ANONYMOUS),
                review.getRating(), review.getComment(), review.getCreatedAt()));
    }

    /**
     * Gives or changes the stars. A comment written earlier stays.
     *
     * @throws SelfReviewException      if the reviewer is the provider
     * @throws IllegalArgumentException if the rating is not from 1 to 5
     */
    @Transactional
    public Review rate(Long providerId, Long reviewerId, int rating) {
        if (providerId.equals(reviewerId)) {
            throw new SelfReviewException();
        }
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating must be from " + MIN_RATING + " to " + MAX_RATING);
        }
        Optional<Review> existing = reviews.findByProviderIdAndReviewerId(providerId, reviewerId);
        if (existing.isPresent()) {
            existing.get().changeRating(rating);
            return existing.get();
        }
        return reviews.save(new Review(providerId, reviewerId, rating));
    }

    /**
     * Writes, replaces or (with a blank text) removes the comment on the reviewer's own review.
     * Returns false if they have not rated this provider yet.
     *
     * @throws IllegalArgumentException if the comment is longer than {@link #MAX_COMMENT_LENGTH}
     */
    @Transactional
    public boolean comment(Long providerId, Long reviewerId, String text) {
        String cleaned = text == null || text.isBlank() ? null : text.strip();
        if (cleaned != null && cleaned.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment is longer than " + MAX_COMMENT_LENGTH + " characters");
        }
        return reviews.findByProviderIdAndReviewerId(providerId, reviewerId)
                .map(review -> {
                    review.changeComment(cleaned);
                    return true;
                })
                .orElse(false);
    }

    /** Removes the reviewer's own review. Returns whether there was one. */
    @Transactional
    public boolean deleteOwn(Long providerId, Long reviewerId) {
        return reviews.findByProviderIdAndReviewerId(providerId, reviewerId)
                .map(review -> {
                    reviews.delete(review);
                    return true;
                })
                .orElse(false);
    }

    private Map<Long, String> namesOf(List<Review> found) {
        List<Long> ids = found.stream().map(Review::getReviewerId).distinct().toList();
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, ReviewService::displayName, (a, b) -> a));
    }

    private static String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? ANONYMOUS : name;
    }
}
