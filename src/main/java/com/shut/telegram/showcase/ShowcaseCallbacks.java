package com.shut.telegram.showcase;

import com.shut.showcase.ShowcaseCategory;

/** Callback data for the showcase buttons. Format: {@code showcase:<action>[:<argument>...]}. */
final class ShowcaseCallbacks {

    static final String PREFIX = "showcase";
    static final String HOME = PREFIX + ":home";

    private ShowcaseCallbacks() {
    }

    static String category(ShowcaseCategory category) {
        return PREFIX + ":cat:" + category.name();
    }

    static String add(ShowcaseCategory category) {
        return PREFIX + ":add:" + category.name();
    }

    static String done(ShowcaseCategory category) {
        return PREFIX + ":done:" + category.name();
    }

    static String view(ShowcaseCategory category, int page) {
        return PREFIX + ":view:" + category.name() + ":" + page;
    }

    static String delete(long itemId) {
        return PREFIX + ":del:" + itemId;
    }

    static String confirmDelete(long itemId) {
        return PREFIX + ":delok:" + itemId;
    }

    static String keep(long itemId) {
        return PREFIX + ":delno:" + itemId;
    }
}
