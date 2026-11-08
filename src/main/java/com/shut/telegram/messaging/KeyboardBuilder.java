package com.shut.telegram.messaging;

import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

/** Builds an inline keyboard row by row. */
public final class KeyboardBuilder {

    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    public KeyboardBuilder row(InlineKeyboardButton... buttons) {
        rows.add(List.of(buttons));
        return this;
    }

    /** Lays the buttons out in rows of {@code perRow}. */
    public KeyboardBuilder grid(List<InlineKeyboardButton> buttons, int perRow) {
        for (int start = 0; start < buttons.size(); start += perRow) {
            rows.add(List.copyOf(buttons.subList(start, Math.min(start + perRow, buttons.size()))));
        }
        return this;
    }

    public InlineKeyboardMarkup build() {
        return new InlineKeyboardMarkup(List.copyOf(rows));
    }
}
