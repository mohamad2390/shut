package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.browse.RankedProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state the members widget draws: {@code {"h": heading, "i": [[name, city, score, [skill...]]...], "c": pages, "p": page}}.
 * The score is the average rating times ten (45 for 4.5), or 0 for a member nobody has rated yet.
 * Names, cities and skills are cut short so that five rows stay under the 1024 bytes Telegram allows.
 */
public final class MemberListWidgetState {

    public static final int PAGE_SIZE = 5;
    static final int MAX_BYTES = 1024;
    static final String HEADING = "متخصص‌ها";
    private static final int MAX_QUERY_LENGTH = 18;

    private static final int MAX_NAME_LENGTH = 24;
    private static final int MAX_CITY_LENGTH = 12;
    private static final int MAX_SKILL_LENGTH = 12;
    private static final int MAX_SKILLS = 3;
    private static final ObjectMapper JSON = new ObjectMapper();

    private MemberListWidgetState() {
    }

    /**
     * @param page the widget counts pages from 1; {@code pages} is at least 1 even when nobody is listed.
     *             A row is {@code [name, city, score, [skills], userId]}.
     */
    public static String page(String query, List<RankedProvider> members, int pages, int page) {
        String state = write(query, members, pages, page, true);
        // Half-spaces take three bytes a letter; if a page still does not fit, it loses the skills, not the rows.
        return state.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES ? state : write(query, members, pages, page, false);
    }

    private static String write(String query, List<RankedProvider> members, int pages, int page, boolean withSkills) {
        List<Object> rows = new ArrayList<>();
        members.forEach(member -> rows.add(List.of(shorten(member.displayName(), MAX_NAME_LENGTH),
                member.city() == null ? "" : shorten(member.city(), MAX_CITY_LENGTH), score(member),
                withSkills ? member.skills().stream().limit(MAX_SKILLS).map(skill -> shorten(skill, MAX_SKILL_LENGTH)).toList()
                        : List.of(),
                member.userId())));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", heading(query));
        state.put("i", rows);
        state.put("c", Math.max(1, pages));
        state.put("p", page);
        try {
            return JSON.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }

    /** "متخصص‌ها" for everyone, or "نتیجه‌ی «words»" for a search. */
    static String heading(String query) {
        return query == null || query.isBlank() ? HEADING
                : "نتیجه‌ی «" + shorten(query.strip(), MAX_QUERY_LENGTH) + "»";
    }

    static int score(RankedProvider member) {
        return member.ratings() == 0 ? 0 : (int) Math.round(member.average() * 10);
    }

    private static String shorten(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text;
    }
}
