package com.shut.venue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VenueTest {

    @Test
    void aVenueIsListableOnlyWithACityAndWhenActive() {
        Venue venue = new Venue(1L, "باغ");
        assertThat(venue.isListable()).isFalse();

        venue.setCity("کرج");
        assertThat(venue.isListable()).isTrue();

        venue.setActive(false);
        assertThat(venue.isListable()).isFalse();
    }
}
