package com.shut.showcase;

import java.util.Optional;

/** The sections a portfolio is divided into. */
public enum ShowcaseCategory {
    PHOTOGRAPHY("عکاسی"),
    VIDEOGRAPHY("فیلمبرداری"),
    PHOTO_EDITING("ادیت عکس"),
    VIDEO_EDITING("تدوین"),
    AERIAL("تصویربرداری هوایی"),
    TEASER("تیزر"),
    OTHER("سایر");

    private final String label;

    ShowcaseCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ShowcaseCategory> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
