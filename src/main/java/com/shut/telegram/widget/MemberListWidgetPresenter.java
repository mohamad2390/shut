package com.shut.telegram.widget;

import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** Shows all members, best rated first, in the members widget. */
@Component
@TelegramBotEnabled
public class MemberListWidgetPresenter {

    private static final int MAX_QUERY_LENGTH = 200;

    private final WidgetDelivery delivery;
    private final WidgetHtml html;
    private final MemberListWidgetAnswerer answerer;

    MemberListWidgetPresenter(WidgetDelivery delivery, WidgetHtml html, MemberListWidgetAnswerer answerer) {
        this.delivery = delivery;
        this.html = html;
        this.answerer = answerer;
    }

    /** Everyone, best rated first. */
    public boolean show(long chatId) {
        return show(chatId, null);
    }

    /**
     * Sends the widget with the members matching {@code query} (everyone if it is blank), replacing the one this chat
     * got earlier. False means it could not be sent or nobody matches, and the caller shows the text version.
     */
    public boolean show(long chatId, String query) {
        if (html.members() == null) {
            return false;
        }
        String words = query == null || query.isBlank() ? null : query.strip();
        if (words != null && words.length() > MAX_QUERY_LENGTH) {
            words = words.substring(0, MAX_QUERY_LENGTH);
        }
        String searched = words;
        return answerer.firstState(searched)
                .map(state -> delivery.replaceShared(chatId, WidgetKind.MEMBERS, html.members(), state, searched))
                .orElse(false);
    }
}
