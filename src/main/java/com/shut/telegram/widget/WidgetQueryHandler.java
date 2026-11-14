package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.model.Update;
import com.shut.telegram.model.WidgetQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Answers the taps inside the widgets. Telegram only says which message was tapped, so the message is looked up to
 * find out which kind of widget it is and whose it is; the matching {@link WidgetAnswerer} builds the new state.
 */
@Component
@TelegramBotEnabled
class WidgetQueryHandler implements UpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(WidgetQueryHandler.class);
    /** What a widget is answered with after a tap that opened something else; the widget ignores it. */
    private static final String OPENED = "{}";
    private static final String UNAVAILABLE = "این ویجت دیگه در دسترس نیست";

    private final TelegramApiClient telegram;
    private final MembershipGate membershipGate;
    private final WidgetMessageRepository widgets;
    private final ProviderSearchService providers;
    private final Map<WidgetKind, WidgetAnswerer> answerers = new EnumMap<>(WidgetKind.class);
    private final Map<WidgetKind, SharedWidgetAnswerer> sharedAnswerers = new EnumMap<>(WidgetKind.class);

    WidgetQueryHandler(TelegramApiClient telegram, MembershipGate membershipGate, WidgetMessageRepository widgets,
                       ProviderSearchService providers, List<WidgetAnswerer> answerers,
                       List<SharedWidgetAnswerer> sharedAnswerers) {
        this.telegram = telegram;
        this.membershipGate = membershipGate;
        this.widgets = widgets;
        this.providers = providers;
        answerers.forEach(answerer -> this.answerers.put(answerer.kind(), answerer));
        sharedAnswerers.forEach(answerer -> this.sharedAnswerers.put(answerer.kind(), answerer));
    }

    @Override
    public boolean supports(Update update) {
        return update.widgetQuery() != null;
    }

    @Override
    public void handle(Update update) {
        WidgetQuery query = update.widgetQuery();
        try {
            String answer = answerFor(query);
            telegram.answerWidgetQuery(query.id(), answer);
            afterAnswer(query);
            log.info("Answered widget query '{}' on message {} in chat {} ({} bytes)", query.data(), query.messageId(),
                    query.chat() == null ? null : query.chat().id(), answer.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        } catch (RuntimeException e) {
            log.warn("Could not answer widget query '{}' on message {}: {}", query.data(), query.messageId(), e.getMessage(), e);
        }
    }

    private void afterAnswer(WidgetQuery query) {
        if (query.messageId() == null || query.chat() == null) {
            return;
        }
        widgets.findByChatIdAndMessageId(query.chat().id(), query.messageId())
                .map(message -> sharedAnswerers.get(message.getKind()) == null ? null : message)
                .ifPresent(message -> sharedAnswerers.get(message.getKind())
                        .afterAnswer(query.chat().id(), message, query.data()));
    }

    /** The state to answer with. Every query must get an answer, so problems become an empty state, not an error. */
    private String answerFor(WidgetQuery query) {
        if (query.messageId() == null || query.chat() == null || query.from() == null
                || !membershipGate.admit(query.from().id(), query.chat().id())) {
            return PriceWidgetState.empty(UNAVAILABLE);
        }
        Optional<WidgetMessage> message = widgets.findByChatIdAndMessageId(query.chat().id(), query.messageId());
        if (message.isPresent() && sharedAnswerers.containsKey(message.get().getKind())) {
            SharedWidgetAnswerer shared = sharedAnswerers.get(message.get().getKind());
            return shared.open(query.chat().id(), message.get(), query.data())
                    ? OPENED : shared.answer(message.get(), query.data());
        }
        Optional<ProviderDetails> provider = message.flatMap(widget -> providers.find(widget.getProviderId()));
        if (provider.isEmpty()) {
            return PriceWidgetState.empty(UNAVAILABLE);
        }
        WidgetAnswerer answerer = answerers.get(message.get().getKind());
        return answerer.open(query.chat().id(), provider.get(), query.data())
                ? OPENED : answerer.answer(provider.get(), query.data());
    }
}
