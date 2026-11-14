package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.review.RatingSummary;
import com.shut.review.ReviewView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state of the reviews widget:
 * {@code {"h": name, "r": average times ten, "n": ratings, "i": [[reviewer, stars, comment]...], "c": pages, "p": page}}.
 * Comments can be 500 letters, so they are cut short; if a page still does not fit in the 1024 bytes Telegram
 * allows, the comments are cut harder.
 */
final class ReviewsWidgetState {

    static final int PAGE_SIZE = 3;
    static final int MAX_BYTES = 1024;

    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_REVIEWER_LENGTH = 14;
    private static final int COMMENT_LENGTH = 90;
    private static final int SHORT_COMMENT_LENGTH = 25;
    private static final ObjectMapper JSON = new ObjectMapper();

    private ReviewsWidgetState() {
    }

    /** @param page counted from 1; {@code pages} is at least 1 even when there are no reviews */
    static String page(String name, RatingSummary rating, List<ReviewView> reviews, int pages, int page) {
        String state = write(name, rating, reviews, pages, page, COMMENT_LENGTH);
        return state.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
                ? state : write(name, rating, reviews, pages, page, SHORT_COMMENT_LENGTH);
    }

    private static String write(String name, RatingSummary rating, List<ReviewView> reviews, int pages, int page,
                                int commentLength) {
        List<Object> rows = new ArrayList<>();
        reviews.forEach(review -> rows.add(List.of(cut(review.reviewerName(), MAX_REVIEWER_LENGTH), review.rating(),
                review.comment() == null ? "" : cut(review.comment(), commentLength))));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", cut(name, MAX_NAME_LENGTH));
        state.put("r", rating.isEmpty() ? 0 : (int) Math.round(rating.average() * 10));
        state.put("n", rating.count());
        state.put("i", rows);
        state.put("c", Math.max(1, pages));
        state.put("p", page);
        try {
            return JSON.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }

    private static String cut(String text, int maxLength) {
        String clean = text.strip().replaceAll("\\s+", " ");
        return clean.length() > maxLength ? clean.substring(0, maxLength - 1) + "…" : clean;
    }
}
