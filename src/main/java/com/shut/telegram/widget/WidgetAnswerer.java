package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;

/** Answers the taps inside one kind of widget. */
interface WidgetAnswerer {

    WidgetKind kind();

    /**
     * The state to answer with.
     *
     * @param provider the member the widget is about
     * @param request  what the widget asked for, such as {@code "i"} or {@code "p:3"}
     */
    String answer(ProviderDetails provider, String request);

    /**
     * Handles a tap that opens something else in the chat, such as another widget. Returns true if the request was
     * one of those, in which case the widget keeps what it shows and is answered with an empty state.
     */
    default boolean open(long chatId, ProviderDetails provider, String request) {
        return false;
    }
}
