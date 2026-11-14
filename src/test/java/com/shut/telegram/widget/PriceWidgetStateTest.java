package com.shut.telegram.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingUnit;
import com.shut.pricing.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PriceWidgetStateTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<PricingUnit, String> ICONS = new EnumMap<>(Map.of(
            PricingUnit.HOUR, "⏱", PricingUnit.DAY, "📅", PricingUnit.PHOTO, "📷",
            PricingUnit.VIDEO_MINUTE, "🎬", PricingUnit.PROJECT, "📦"));

    private static PriceItem item(long id, String title, PricingUnit unit, long amount) {
        PriceItem item = new PriceItem(1L, title, unit, amount);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static List<PriceItem> items(int count, String title) {
        List<PriceItem> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(item(i, title + " " + i, PricingUnit.values()[i % PricingUnit.values().length], 1_000_000L * i));
        }
        return items;
    }

    private static JsonNode parse(String json) throws Exception {
        return JSON.readTree(json);
    }

    @Test
    void aRowHasTheSevenFieldsTheWidgetReads() throws Exception {
        JsonNode state = parse(PriceWidgetState.page("قیمت‌های علی",
                List.of(item(7, "عکاسی عروسی", PricingUnit.HOUR, 1_000_000)), ICONS, 1));

        assertThat(state.get("h").asText()).isEqualTo("قیمت‌های علی");
        JsonNode row = state.get("i").get(0);
        assertThat(row).hasSize(7);
        assertThat(row.get(0).asLong()).isEqualTo(7);
        assertThat(row.get(2).asText()).isEqualTo("عکاسی عروسی");
        assertThat(row.get(3).asLong()).isEqualTo(1_000_000);
        assertThat(row.get(4).asText()).isEqualTo("ساعت");
        assertThat(row.get(5).asInt()).isZero();
        assertThat(row.get(1).asText()).isEqualTo(PricingCatalog.DEFAULT_SERVICE_ICON);
        assertThat(row.get(6).asText()).isEqualTo("⏱");
        assertThat(state.get("c").asInt()).isEqualTo(1);
        assertThat(state.get("p").asInt()).isEqualTo(1);
    }

    @Test
    void unitsAreBareBecauseTheWidgetWritesHorBeforeThem() {
        assertThat(PriceWidgetState.unit(PricingUnit.HOUR)).isEqualTo("ساعت");
        assertThat(PriceWidgetState.unit(PricingUnit.DAY)).isEqualTo("روز");
        assertThat(PriceWidgetState.unit(PricingUnit.PHOTO)).isEqualTo("عکس");
        assertThat(PriceWidgetState.unit(PricingUnit.VIDEO_MINUTE)).isEqualTo("دقیقه ویدیو");
    }

    @Test
    void aProjectPriceIsMarkedFixedSoTheWidgetSaysFixedPrice() {
        assertThat(PriceWidgetState.unit(PricingUnit.PROJECT)).isEqualTo("ثابت");
    }

    @Test
    void everyPageFitsTelegramsLimitEvenWithLongTitles() throws Exception {
        List<PriceItem> items = items(40, "عکاسی محصول برای فروشگاه‌های اینترنتی با نور طبیعی و استودیویی");
        int pages = PriceWidgetState.pageCount("قیمت‌های یک استودیوی خیلی معروف و بزرگ", items, ICONS);

        int rows = 0;
        for (int page = 1; page <= pages; page++) {
            String json = PriceWidgetState.page("قیمت‌های یک استودیوی خیلی معروف و بزرگ", items, ICONS, page);
            assertThat(json.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(PriceWidgetState.MAX_BYTES);
            JsonNode state = parse(json);
            assertThat(state.get("i").size()).isLessThanOrEqualTo(PriceWidgetState.MAX_ITEMS_PER_PAGE);
            assertThat(state.get("p").asInt()).isEqualTo(page);
            assertThat(state.get("c").asInt()).isEqualTo(pages);
            rows += state.get("i").size();
        }
        assertThat(pages).isGreaterThan(1);
        assertThat(rows).isEqualTo(40);
    }

    @Test
    void pagesKeepTheOrderOfThePriceMenu() throws Exception {
        List<PriceItem> items = items(20, "خدمت");
        List<Long> ids = new ArrayList<>();
        for (int page = 1; page <= PriceWidgetState.pageCount("h", items, ICONS); page++) {
            parse(PriceWidgetState.page("h", items, ICONS, page)).get("i").forEach(row -> ids.add(row.get(0).asLong()));
        }

        assertThat(ids).containsExactlyElementsOf(items.stream().map(PriceItem::getId).toList());
    }

    @Test
    void aPageOutsideTheRangeIsMovedToTheNearestOne() throws Exception {
        List<PriceItem> items = items(20, "خدمت");
        int pages = PriceWidgetState.pageCount("h", items, ICONS);

        assertThat(parse(PriceWidgetState.page("h", items, ICONS, 0)).get("p").asInt()).isEqualTo(1);
        assertThat(parse(PriceWidgetState.page("h", items, ICONS, 999)).get("p").asInt()).isEqualTo(pages);
    }

    @Test
    void noPricesGivesOneEmptyPage() throws Exception {
        JsonNode state = parse(PriceWidgetState.page("قیمت‌های علی", List.of(), ICONS, 1));

        assertThat(state.get("i")).isEmpty();
        assertThat(state.get("c").asInt()).isEqualTo(1);
    }

    @Test
    void theEmptyStateCarriesJustAMessage() throws Exception {
        JsonNode state = parse(PriceWidgetState.empty("این قیمت‌ها دیگه در دسترس نیست"));

        assertThat(state.get("h").asText()).isEqualTo("این قیمت‌ها دیگه در دسترس نیست");
        assertThat(state.get("i")).isEmpty();
    }

    @Test
    void aLongHeadingIsShortened() throws Exception {
        JsonNode state = parse(PriceWidgetState.empty("x".repeat(200)));

        assertThat(state.get("h").asText().length()).isLessThanOrEqualTo(36);
    }

    @Test
    void theRowIconIsTheOneOfTheServiceAndTheUnitIconComesFromTheTable() throws Exception {
        PriceItem item = new PriceItem(1L, new ServiceType("عکاسی عروسی", "💍"), PricingUnit.PHOTO, 90_000);
        ReflectionTestUtils.setField(item, "id", 3L);

        JsonNode row = parse(PriceWidgetState.page("h", List.of(item), ICONS, 1)).get("i").get(0);
        JsonNode changed = parse(PriceWidgetState.page("h", List.of(item), Map.of(PricingUnit.PHOTO, "🖼"), 1)).get("i").get(0);

        assertThat(row.get(1).asText()).isEqualTo("💍");
        assertThat(row.get(6).asText()).isEqualTo("📷");
        assertThat(changed.get(6).asText()).isEqualTo("🖼");
    }

    @Test
    void aUnitWithoutAnIconGetsTheDefaultOne() throws Exception {
        JsonNode row = parse(PriceWidgetState.page("h", List.of(item(1, "عکاسی", PricingUnit.DAY, 1)), Map.of(), 1)).get("i").get(0);

        assertThat(row.get(6).asText()).isEqualTo(PricingCatalog.DEFAULT_UNIT_ICON);
    }
}
