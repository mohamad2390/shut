package com.shut.telegram.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.browse.RankedProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MemberListWidgetStateTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void eachRowHasOnlyNameCityAndScore() throws Exception {
        String state = MemberListWidgetState.page(null,
                List.of(new RankedProvider(1L, "علی", "شیراز", 4.5, 3, List.of("پرتره", "تدوین"))), 2, 1);

        JsonNode json = JSON.readTree(state);
        assertThat(json.get("i").get(0).toString()).isEqualTo("[\"علی\",\"شیراز\",45,[\"پرتره\",\"تدوین\"],1]");
        assertThat(json.get("c").asInt()).isEqualTo(2);
        assertThat(json.get("p").asInt()).isEqualTo(1);
    }

    @Test
    void aMemberWithoutRatingsHasScoreZeroAndNoCityBecomesEmpty() throws Exception {
        String state = MemberListWidgetState.page(null,
                List.of(new RankedProvider(1L, "سارا", null, 0, 0, List.of())), 1, 1);

        assertThat(JSON.readTree(state).get("i").get(0).toString()).isEqualTo("[\"سارا\",\"\",0,[],1]");
    }

    @Test
    void anEmptyListStillHasOnePage() throws Exception {
        assertThat(JSON.readTree(MemberListWidgetState.page(null, List.of(), 0, 1)).get("c").asInt()).isEqualTo(1);
    }

    @Test
    void aPageOfLongNamesFitsInTelegramsLimit() {
        List<RankedProvider> members = IntStream.range(0, MemberListWidgetState.PAGE_SIZE)
                .mapToObj(i -> new RankedProvider((long) i, "ن".repeat(80), "ش".repeat(80), 4.4, 9,
                        List.of("م".repeat(80), "م".repeat(80), "م".repeat(80)))).toList();

        String state = MemberListWidgetState.page("ک".repeat(80), members, 99, 99);

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void onlyTheFirstThreeSkillsAreKept() throws Exception {
        String state = MemberListWidgetState.page(null, List.of(
                new RankedProvider(1L, "علی", "یزد", 4, 1, List.of("الف", "ب", "پ", "ت"))), 1, 1);

        assertThat(JSON.readTree(state).get("i").get(0).get(3)).hasSize(3);
    }

    @Test
    void aSearchNamesItsWordsInTheHeading() throws Exception {
        String state = MemberListWidgetState.page("ترکیه", List.of(), 1, 1);

        assertThat(JSON.readTree(state).get("h").asText()).isEqualTo("نتیجه‌ی «ترکیه»");
    }

    @Test
    void aPageThatStillDoesNotFitLosesItsSkillsButKeepsItsRows() throws Exception {
        List<RankedProvider> members = IntStream.range(0, MemberListWidgetState.PAGE_SIZE)
                .mapToObj(i -> new RankedProvider((long) i, "\u200c".repeat(24), "\u200c".repeat(12), 4.4, 9,
                        List.of("\u200c".repeat(12), "\u200c".repeat(12), "\u200c".repeat(12)))).toList();

        String state = MemberListWidgetState.page(null, members, 1, 1);

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
        JsonNode rows = JSON.readTree(state).get("i");
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).get(3)).isEmpty();
    }
}
