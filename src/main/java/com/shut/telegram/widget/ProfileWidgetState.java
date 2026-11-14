package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.browse.ProviderDetails;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.review.RatingSummary;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The state of the profile widget:
 * {@code {"h": name, "t": headline, "l": location, "y": years, "u": telegram, "g": instagram, "k": [skills],
 * "e": [gear], "m": more gear, "b": bio, "r": average times ten, "n": ratings}}. Missing parts are left out. Telegram allows 1024 bytes, so
 * everything is cut short, and the bio and then some skills go first if a profile still does not fit.
 */
final class ProfileWidgetState {

    static final int MAX_BYTES = 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    private ProfileWidgetState() {
    }

    static String of(ProviderDetails details, RatingSummary rating) {
        return of(details, rating, List.of());
    }

    /** {@code gear} is the member's listed equipment, one short line each. */
    static String of(ProviderDetails details, RatingSummary rating, List<String> gear) {
        String state = write(details, rating, gear, 150, 6, 5);
        if (bytes(state) > MAX_BYTES) {
            state = write(details, rating, gear, 0, 6, 5);
        }
        if (bytes(state) > MAX_BYTES) {
            state = write(details, rating, gear, 0, 3, 3);
        }
        if (bytes(state) > MAX_BYTES) {
            state = write(details, rating, gear, 0, 3, 0);
        }
        return state;
    }

    private static String write(ProviderDetails details, RatingSummary rating, List<String> gear, int bioLength,
                                int skillCount, int gearCount) {
        ProfessionalProfileResponse profile = details.profile();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", cut(WidgetNames.of(details), 30));
        put(state, "t", cut(profile.headline(), 50));
        put(state, "l", location(profile));
        if (profile.yearsExperience() != null && profile.yearsExperience() > 0) {
            state.put("y", profile.yearsExperience());
        }
        put(state, "u", cut(details.telegramUsername(), 32));
        put(state, "g", cut(profile.instagramUsername(), 32));
        List<String> skills = profile.skills().stream().sorted().limit(skillCount).map(skill -> cut(skill, 14)).toList();
        if (!skills.isEmpty()) {
            state.put("k", skills);
        }
        List<String> shownGear = gear.stream().limit(gearCount).map(item -> cut(item, 26)).toList();
        if (!shownGear.isEmpty()) {
            state.put("e", shownGear);
            if (gear.size() > shownGear.size()) {
                state.put("m", gear.size() - shownGear.size());
            }
        }
        if (bioLength > 0) {
            put(state, "b", cut(profile.bio(), bioLength));
        }
        state.put("r", rating.isEmpty() ? 0 : (int) Math.round(rating.average() * 10));
        state.put("n", rating.count());
        try {
            return JSON.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }

    private static String location(ProfessionalProfileResponse profile) {
        String text = Stream.of(profile.baseCity(), profile.baseCountry())
                .filter(Objects::nonNull).map(String::strip).filter(part -> !part.isEmpty())
                .reduce((city, country) -> city + "، " + country).orElse("");
        return cut(text, 30);
    }

    private static void put(Map<String, Object> state, String key, String value) {
        if (value != null && !value.isBlank()) {
            state.put(key, value);
        }
    }

    private static String cut(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String clean = text.strip().replaceAll("\\s+", " ");
        return clean.length() > maxLength ? clean.substring(0, maxLength - 1) + "…" : clean;
    }

    private static int bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8).length;
    }
}
