package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.showcase.ShowcaseCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The state of the showcase widget: {@code {"h": name, "i": [[categoryKey, label, count]...]}}, filled categories only. */
final class ShowcaseWidgetState {

    private static final int MAX_NAME_LENGTH = 30;
    private static final ObjectMapper JSON = new ObjectMapper();

    private ShowcaseWidgetState() {
    }

    static String of(String name, Map<ShowcaseCategory, Long> counts) {
        List<Object> rows = new ArrayList<>();
        for (ShowcaseCategory category : ShowcaseCategory.values()) {
            long count = counts.getOrDefault(category, 0L);
            if (count > 0) {
                rows.add(List.of(category.name(), category.label(), count));
            }
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH - 1) + "…" : name);
        state.put("i", rows);
        try {
            return JSON.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }
}
