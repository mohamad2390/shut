package com.shut.telegram.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingUnit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state the price widget draws, as the JSON it expects:
 * {@code {"h": heading, "i": [[id, icon, name, price, unit, openable]...], "c": pages, "p": page}}.
 *
 * <p>Telegram caps a widget's state, and every answer to it, at 1024 bytes. Persian text takes two bytes per letter,
 * so prices are split into pages by how many bytes they take, not by a fixed count.
 */
public final class PriceWidgetState {

    /** What Telegram allows for one state or answer. */
    static final int MAX_BYTES = 1024;
    /** Five rows are about 485 px tall in the widget; Telegram clips a widget at 600 px. */
    static final int MAX_ITEMS_PER_PAGE = 5;

    private static final int MAX_TITLE_LENGTH = 40;
    private static final int MAX_HEADING_LENGTH = 36;
    /** What the widget shows for a price that does not depend on an amount of work. */
    private static final String FIXED_UNIT = "ثابت";
    private static final ObjectMapper JSON = new ObjectMapper();

    private PriceWidgetState() {
    }

    /** How many pages the prices take. A member with no prices still has one (empty) page. */
    public static int pageCount(String heading, List<PriceItem> items) {
        return split(heading, items).size();
    }

    /**
     * One page of prices. The widget counts pages from 1; a page outside the range is moved to the nearest one.
     *
     * @param heading what the widget shows above the rows, for example "قیمت‌های علی"
     */
    public static String page(String heading, List<PriceItem> items, int page) {
        List<List<PriceItem>> pages = split(heading, items);
        int index = Math.max(1, Math.min(page, pages.size())) - 1;
        return json(heading, pages.get(index), pages.size(), index + 1);
    }

    /** A state with a heading and no rows; the widget says nothing is listed. */
    public static String empty(String heading) {
        return json(heading, List.of(), 1, 1);
    }

    private static List<List<PriceItem>> split(String heading, List<PriceItem> items) {
        int budget = MAX_BYTES - bytes(json(heading, List.of(), 99, 99)) - 16;
        List<List<PriceItem>> pages = new ArrayList<>();
        List<PriceItem> current = new ArrayList<>();
        int used = 0;
        for (PriceItem item : items) {
            int size = bytes(row(item)) + 1;
            boolean full = used + size > budget || current.size() >= MAX_ITEMS_PER_PAGE;
            if (full && !current.isEmpty()) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(item);
            used += size;
        }
        pages.add(current);
        return pages;
    }

    private static String json(String heading, List<PriceItem> items, int pages, int page) {
        List<Object> rows = new ArrayList<>();
        items.forEach(item -> rows.add(row(item)));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("h", shorten(heading, MAX_HEADING_LENGTH));
        state.put("i", rows);
        state.put("c", pages);
        state.put("p", page);
        return write(state);
    }

    /** A row is {@code [id, icon, name, price, unit, openable]}; prices are not openable. */
    private static List<Object> row(PriceItem item) {
        return List.of(item.getId(), icon(item.getUnit()), shorten(item.getTitle(), MAX_TITLE_LENGTH),
                item.getAmount(), unit(item.getUnit()), 0);
    }

    /** The widget writes "هر" before this, so it is the bare unit: "ساعت", not "هر ساعت". */
    static String unit(PricingUnit unit) {
        return switch (unit) {
            case HOUR -> "ساعت";
            case DAY -> "روز";
            case PHOTO -> "عکس";
            case VIDEO_MINUTE -> "دقیقه ویدیو";
            case PROJECT -> FIXED_UNIT;
        };
    }

    static String icon(PricingUnit unit) {
        return switch (unit) {
            case HOUR -> "⏱";
            case DAY -> "📅";
            case PHOTO -> "📷";
            case VIDEO_MINUTE -> "🎬";
            case PROJECT -> "📦";
        };
    }

    private static String shorten(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text;
    }

    private static int bytes(Object value) {
        return write(value).getBytes(StandardCharsets.UTF_8).length;
    }

    private static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write widget state", e);
        }
    }
}
