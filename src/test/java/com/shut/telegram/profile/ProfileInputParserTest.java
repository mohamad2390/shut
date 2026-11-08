package com.shut.telegram.profile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileInputParserTest {

    @Test
    void readsPersianDigitsForYears() {
        assertThat(ProfileInputParser.years("۸")).isEqualTo(8);
    }

    @Test
    void rejectsYearsOutOfRangeOrNotANumber() {
        assertThatThrownBy(() -> ProfileInputParser.years("81")).isInstanceOf(InvalidProfileInputException.class);
        assertThatThrownBy(() -> ProfileInputParser.years("abc")).isInstanceOf(InvalidProfileInputException.class);
    }

    @Test
    void splitsListsOnCommasAndNewlinesAndDropsDuplicates() {
        assertThat(ProfileInputParser.list("عکاسی، تدوین, ادیت\nتدوین", 15, 50))
                .containsExactlyElementsOf(List.of("عکاسی", "تدوین", "ادیت"));
    }

    @Test
    void dashClearsAField() {
        assertThat(ProfileInputParser.text(" - ", 10)).isEmpty();
        assertThat(ProfileInputParser.list("-", 15, 50)).isEmpty();
        assertThat(ProfileInputParser.years("-")).isZero();
    }

    @Test
    void rejectsTooManyListItemsAndTooLongText() {
        assertThatThrownBy(() -> ProfileInputParser.list("a,b,c", 2, 50)).isInstanceOf(InvalidProfileInputException.class);
        assertThatThrownBy(() -> ProfileInputParser.text("x".repeat(11), 10)).isInstanceOf(InvalidProfileInputException.class);
    }

    @Test
    void normalisesWebsiteAndInstagram() {
        assertThat(ProfileInputParser.website("example.com")).isEqualTo("https://example.com");
        assertThat(ProfileInputParser.instagram("@ali.photo")).isEqualTo("ali.photo");
        assertThat(ProfileInputParser.instagram("https://www.instagram.com/ali_a/")).isEqualTo("ali_a");
        assertThatThrownBy(() -> ProfileInputParser.website("not a url")).isInstanceOf(InvalidProfileInputException.class);
        assertThatThrownBy(() -> ProfileInputParser.instagram("علی")).isInstanceOf(InvalidProfileInputException.class);
    }
}
