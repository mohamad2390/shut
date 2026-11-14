package com.shut.telegram.venue;

import com.shut.venue.Venue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VenueFieldTest {

    private final Venue venue = new Venue(1L, "باغ");

    @Test
    void aDashClearsAnOptionalField() {
        venue.setAddress("خیابان اول");

        VenueField.ADDRESS.applyTo(venue, "-");

        assertThat(venue.getAddress()).isNull();
    }

    @Test
    void theNameCannotBeEmptyOrTooLong() {
        assertThatThrownBy(() -> VenueField.NAME.applyTo(venue, "   ")).isInstanceOf(InvalidVenueInputException.class);
        assertThatThrownBy(() -> VenueField.NAME.applyTo(venue, "ا".repeat(101))).isInstanceOf(InvalidVenueInputException.class);
    }

    @Test
    void phoneIsStoredWithLatinDigits() {
        VenueField.PHONE.applyTo(venue, "۰۹۱۲۱۲۳۴۵۶۷");

        assertThat(venue.getPhone()).isEqualTo("09121234567");
    }

    @Test
    void phoneWithLettersIsRejected() {
        assertThatThrownBy(() -> VenueField.PHONE.applyTo(venue, "زنگ بزن")).isInstanceOf(InvalidVenueInputException.class);
    }

    @Test
    void everyFieldKeyIsFoundAgain() {
        for (VenueField field : VenueField.values()) {
            assertThat(VenueField.fromKey(field.name())).contains(field);
        }
    }
}
