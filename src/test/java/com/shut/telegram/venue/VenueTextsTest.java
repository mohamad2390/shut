package com.shut.telegram.venue;

import com.shut.venue.Venue;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenuePriceUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VenueTextsTest {

    private final VenuePrice hourly = new VenuePrice(1L, null, VenuePriceUnit.HOUR, 3_000_000L);
    private final VenuePrice daily = new VenuePrice(1L, null, VenuePriceUnit.DAY, 10_000_000L);
    private final VenuePrice privateDay = new VenuePrice(1L, "خصوصی", VenuePriceUnit.FIXED, 50_000_000L);

    @Test
    void aPriceWithoutAnameShowsAmountAndUnit() {
        assertThat(VenueTexts.priceLine(hourly)).isEqualTo("۳٬۰۰۰٬۰۰۰ تومان، هر ساعت");
    }

    @Test
    void aNamedPriceStartsWithItsName() {
        assertThat(VenueTexts.priceLine(privateDay)).startsWith("خصوصی: ").contains("یک‌جا");
    }

    @Test
    void theSummaryShowsTheFirstPriceAndHintsAtMore() {
        assertThat(VenueTexts.priceSummary(List.of(daily))).doesNotContain("…");
        assertThat(VenueTexts.priceSummary(List.of(daily, hourly))).contains("هر روز").endsWith("و …");
        assertThat(VenueTexts.priceSummary(List.of())).isEqualTo("قیمت ثبت نشده");
    }

    @Test
    void detailsListEveryPrice() {
        Venue venue = new Venue(1L, "باغ");

        String text = VenueTexts.details(venue, 0, List.of(hourly, daily, privateDay));

        assertThat(text).contains("هر ساعت").contains("هر روز").contains("خصوصی");
    }

    @Test
    void aVenueWithNoPriceIsMissingOne() {
        Venue venue = new Venue(1L, "باغ");
        venue.setCity("کرج");

        assertThat(VenueTexts.missing(venue, 0)).isEqualTo("قیمت");
        assertThat(VenueTexts.missing(venue, 1)).isEmpty();
    }
}
