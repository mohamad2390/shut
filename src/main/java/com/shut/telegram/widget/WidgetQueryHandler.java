package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderSearchService;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingService;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.model.Update;
import com.shut.telegram.model.WidgetQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Answers the taps inside the price widget. The widget asks for a page of prices ({@code "i"} for the first,
 * {@code "p:3"} for the third) and draws whatever state comes back.
 */
@Component
@TelegramBotEnabled
class WidgetQueryHandler implements UpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(WidgetQueryHandler.class);
    private static final String PAGE_PREFIX = "p:";
    private static final String UNAVAILABLE = "این قیمت‌ها دیگه در دسترس نیست";

    private final TelegramApiClient telegram;
    private final MembershipGate membershipGate;
    private final PriceWidgetRepository widgets;
    private final ProviderSearchService providers;
    private final PricingService pricing;

    WidgetQueryHandler(TelegramApiClient telegram, MembershipGate membershipGate, PriceWidgetRepository widgets,
                       ProviderSearchService providers, PricingService pricing) {
        this.telegram = telegram;
        this.membershipGate = membershipGate;
        this.widgets = widgets;
        this.providers = providers;
        this.pricing = pricing;
    }

    @Override
    public boolean supports(Update update) {
        return update.widgetQuery() != null;
    }

    @Override
    public void handle(Update update) {
        WidgetQuery query = update.widgetQuery();
        try {
            telegram.answerWidgetQuery(query.id(), answerFor(query));
        } catch (RuntimeException e) {
            log.warn("Could not answer widget query {}: {}", query.id(), e.getMessage());
        }
    }

    /** The state to answer with. Every query must get an answer, so problems become an empty state, not an error. */
    private String answerFor(WidgetQuery query) {
        if (query.messageId() == null || query.chat() == null || query.from() == null
                || !membershipGate.admit(query.from().id(), query.chat().id())) {
            return PriceWidgetState.empty(UNAVAILABLE);
        }
        Optional<ProviderDetails> provider = widgets.findByChatIdAndMessageId(query.chat().id(), query.messageId())
                .flatMap(widget -> providers.find(widget.getProviderId()));
        if (provider.isEmpty()) {
            return PriceWidgetState.empty(UNAVAILABLE);
        }
        List<PriceItem> items = pricing.list(provider.get().userId());
        String heading = PriceWidgetPresenter.heading(provider.get().profile().displayName());
        return PriceWidgetState.page(heading, items, requestedPage(query.data()));
    }

    /** The first page unless the widget asked for another one. */
    private static int requestedPage(String data) {
        if (data != null && data.startsWith(PAGE_PREFIX)) {
            try {
                return Integer.parseInt(data.substring(PAGE_PREFIX.length()));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}
