package com.shut.telegram.venue;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class VenueTimeRangeTest {

    @Test
    void readsPersianAndLatinDigitsWithAnySeparator() {
        assertThat(VenueTimeRange.parse("۹ تا ۱۲")).contains(new VenueTimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertThat(VenueTimeRange.parse("9-12")).contains(new VenueTimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertThat(VenueTimeRange.parse("از 14:30 الی 18")).contains(new VenueTimeRange(LocalTime.of(14, 30), LocalTime.of(18, 0)));
        assertThat(VenueTimeRange.parse("۰۹:۰۰ - ۱۱:۳۰")).contains(new VenueTimeRange(LocalTime.of(9, 0), LocalTime.of(11, 30)));
    }

    @Test
    void twentyFourMeansTheEndOfTheDay() {
        assertThat(VenueTimeRange.parse("20 تا 24")).contains(new VenueTimeRange(LocalTime.of(20, 0), LocalTime.of(23, 59)));
    }

    @Test
    void anythingElseIsRejected() {
        assertThat(VenueTimeRange.parse("12 تا 9")).isEmpty();
        assertThat(VenueTimeRange.parse("9 تا 9")).isEmpty();
        assertThat(VenueTimeRange.parse("9")).isEmpty();
        assertThat(VenueTimeRange.parse("9 تا 12 تا 15")).isEmpty();
        assertThat(VenueTimeRange.parse("25 تا 26")).isEmpty();
        assertThat(VenueTimeRange.parse("9:75 تا 12")).isEmpty();
        assertThat(VenueTimeRange.parse("صبح")).isEmpty();
    }
}
