package com.shut.telegram.widget;

import java.util.EnumSet;
import java.util.Set;

/** The widgets the bot can send. */
public enum WidgetKind {
    PRICES,
    CALENDAR,
    /** The ranked list of all members; it is not about one member. */
    MEMBERS,
    /** The options of one member: profile, calendar, prices, showcase and reviews. */
    PROVIDER_MENU,
    PROFILE,
    SHOWCASE,
    REVIEWS;

    /** What a member's options show: the widgets that open from {@link #PROVIDER_MENU}. */
    private static final Set<WidgetKind> OPTIONS = EnumSet.of(PROFILE, CALENDAR, PRICES, SHOWCASE, REVIEWS);

    /**
     * The kinds of widget that are taken out of a chat when this one is sent, to keep the chat tidy: one option of
     * one member at a time, and a member's menu replaces every other member's menu and options. The members list
     * is never removed.
     */
    Set<WidgetKind> replaces() {
        if (this == PROVIDER_MENU) {
            Set<WidgetKind> all = EnumSet.copyOf(OPTIONS);
            all.add(PROVIDER_MENU);
            return all;
        }
        return OPTIONS.contains(this) ? OPTIONS : Set.of();
    }
}
