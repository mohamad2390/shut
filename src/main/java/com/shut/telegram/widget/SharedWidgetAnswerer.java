package com.shut.telegram.widget;

/** Answers the taps inside a widget that is not about one member, such as the ranked list of members. */
interface SharedWidgetAnswerer {

    WidgetKind kind();

    /** The state to answer with; {@code message} is the widget that was tapped, {@code request} is what the widget asked for, such as {@code "i"} or {@code "p:3"}. */
    String answer(WidgetMessage message, String request);

    /**
     * Runs after the tap has been answered, for what would take the widget's own message away: Telegram does not
     * like a query being answered on a message that is already deleted.
     */
    default void afterAnswer(long chatId, WidgetMessage message, String request) {
    }

    /** As {@link WidgetAnswerer#open}, for a widget that is not about one member. */
    default boolean open(long chatId, WidgetMessage message, String request) {
        return false;
    }
}
