package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.review.RatingSummary;

import java.util.LinkedHashMap;
import java.util.Map;

/** The state of the widget with a member's four options: {@code {"h": name, "r": average times ten, "n": ratings}}. */
final class ProviderMenuWidgetState {

    private static final int MAX_NAME_LENGTH = 30;
    private static final ObjectMapper JSON = new ObjectMapper();

    private ProviderMenuWidgetState() {
    }

    static String of(String name, RatingSummary rating) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH - 1) + "…" : name);
        state.put("r", rating.isEmpty() ? 0 : (int) Math.round(rating.average() * 10));
        state.put("n", rating.count());
        try {
            return JSON.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }
}
