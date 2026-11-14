package com.shut.telegram.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.browse.ProviderDetails;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.review.RatingSummary;
import com.shut.showcase.ShowcaseCategory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAndShowcaseWidgetStateTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static ProviderDetails details(String name, String headline, String bio, Set<String> skills) {
        return new ProviderDetails(new ProfessionalProfileResponse(1L, 7L, name, headline, bio, 5, "وان", "ترکیه",
                null, "ali_ig", null, skills, Set.of(), Set.of(), null, null), "ali");
    }

    @Test
    void theProfileStateHasTheShortFacts() throws Exception {
        String state = ProfileWidgetState.of(details("علی", "عکاس پرتره", "درباره‌ی من", Set.of("تدوین")),
                new RatingSummary(4.5, 3));

        JsonNode json = JSON.readTree(state);
        assertThat(json.get("h").asText()).isEqualTo("علی");
        assertThat(json.get("t").asText()).isEqualTo("عکاس پرتره");
        assertThat(json.get("l").asText()).isEqualTo("وان، ترکیه");
        assertThat(json.get("y").asInt()).isEqualTo(5);
        assertThat(json.get("u").asText()).isEqualTo("ali");
        assertThat(json.get("g").asText()).isEqualTo("ali_ig");
        assertThat(json.get("k")).hasSize(1);
        assertThat(json.get("r").asInt()).isEqualTo(45);
        assertThat(json.get("n").asInt()).isEqualTo(3);
    }

    @Test
    void theGearIsListedAndTheRestCounted() throws Exception {
        List<String> gear = List.of("📷 سونی آلفا ۷", "🔭 سیگما ۳۵", "🚁 دی‌جی‌آی مینی ۴", "💡 گودکس SL60",
                "🎙 رود وایرلس", "🔋 شارژر", "💾 کارت");

        JsonNode json = JSON.readTree(ProfileWidgetState.of(details("علی", null, null, Set.of()), RatingSummary.NONE, gear));

        assertThat(json.get("e")).hasSize(5);
        assertThat(json.get("e").get(0).asText()).isEqualTo("📷 سونی آلفا ۷");
        assertThat(json.get("m").asInt()).isEqualTo(2);
    }

    @Test
    void noGearMeansNoGearKeys() throws Exception {
        JsonNode json = JSON.readTree(ProfileWidgetState.of(details("علی", null, null, Set.of()), RatingSummary.NONE));

        assertThat(json.has("e")).isFalse();
        assertThat(json.has("m")).isFalse();
    }

    @Test
    void aFullProfileWithGearStillFitsInTelegramsLimit() {
        Set<String> skills = IntStream.range(0, 12).mapToObj(i -> "م".repeat(40) + i).collect(Collectors.toSet());
        List<String> gear = IntStream.range(0, 30).mapToObj(i -> "گ".repeat(60) + i).toList();

        String state = ProfileWidgetState.of(details("ن".repeat(80), "ه".repeat(200), "ب".repeat(2000), skills),
                new RatingSummary(4.4, 99), gear);

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void emptyPartsAreLeftOut() throws Exception {
        ProviderDetails bare = new ProviderDetails(new ProfessionalProfileResponse(1L, 7L, "سارا", null, null, 0, null,
                null, null, null, null, Set.of(), Set.of(), Set.of(), null, null), null);

        JsonNode json = JSON.readTree(ProfileWidgetState.of(bare, RatingSummary.NONE));

        assertThat(json.has("t")).isFalse();
        assertThat(json.has("l")).isFalse();
        assertThat(json.has("y")).isFalse();
        assertThat(json.has("u")).isFalse();
        assertThat(json.has("k")).isFalse();
        assertThat(json.get("r").asInt()).isZero();
    }

    @Test
    void aFullProfileFitsInTelegramsLimit() {
        Set<String> skills = IntStream.range(0, 12).mapToObj(i -> "م".repeat(40) + i).collect(Collectors.toSet());

        String state = ProfileWidgetState.of(details("ن".repeat(80), "ه".repeat(200), "ب".repeat(2000), skills),
                new RatingSummary(4.4, 99));

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void aProfileOfHalfSpacesLosesItsBioBeforeBreakingTheLimit() {
        Set<String> skills = IntStream.range(0, 12).mapToObj(i -> "‌".repeat(10) + i).collect(Collectors.toSet());

        String state = ProfileWidgetState.of(details("‌".repeat(30), "‌".repeat(50), "‌".repeat(500), skills),
                new RatingSummary(4.4, 99));

        assertThat(state.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1024);
    }

    @Test
    void theShowcaseStateListsOnlyFilledCategoriesInOrder() throws Exception {
        Map<ShowcaseCategory, Long> counts = new EnumMap<>(ShowcaseCategory.class);
        counts.put(ShowcaseCategory.TEASER, 2L);
        counts.put(ShowcaseCategory.PHOTOGRAPHY, 5L);
        counts.put(ShowcaseCategory.AERIAL, 0L);

        JsonNode json = JSON.readTree(ShowcaseWidgetState.of("علی", counts));

        assertThat(json.get("i").toString()).isEqualTo("[[\"PHOTOGRAPHY\",\"عکاسی\",5],[\"TEASER\",\"تیزر\",2]]");
    }
}
