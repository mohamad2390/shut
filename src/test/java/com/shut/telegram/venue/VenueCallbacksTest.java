package com.shut.telegram.venue;

import com.shut.common.calendar.JalaliMonth;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class VenueCallbacksTest {

    @Test
    void callbackDataFitsTelegramsLimitAndKeepsItsPrefix() {
        LocalDate day = LocalDate.of(2027, 3, 21);
        String[] data = {
                VenueCallbacks.book(123456789L, day), VenueCallbacks.calendar(123456789L, new JalaliMonth(1406, 12)),
                VenueOwnerCallbacks.day(123456789L, day), VenueOwnerCallbacks.edit(123456789L, VenueField.DESCRIPTION),
                VenueOwnerCallbacks.priceUnit(123456789L, "HALF_DAY"), VenueOwnerCallbacks.confirmDelete(123456789L)};

        for (String callback : data) {
            assertThat(callback.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
            assertThat(callback).matches("(venue|vown):.*");
        }
    }

    @Test
    void theTwoHandlersDoNotShareAPrefix() {
        assertThat(VenueOwnerCallbacks.PREFIX + ":").doesNotStartWith(VenueCallbacks.PREFIX + ":");
        assertThat(VenueCallbacks.PREFIX + ":").doesNotStartWith(VenueOwnerCallbacks.PREFIX + ":");
    }

    @Test
    void argsAreReadBack() {
        assertThat(VenueArgs.id("42")).contains(42L);
        assertThat(VenueArgs.id("x")).isEmpty();
        assertThat(VenueArgs.date("2027-03-21")).contains(LocalDate.of(2027, 3, 21));
        assertThat(VenueArgs.date("بد")).isEmpty();
        assertThat(VenueArgs.month("1406", "13")).isEmpty();
        assertThat(VenueArgs.month("1406", "2")).contains(new JalaliMonth(1406, 2));
    }
}
