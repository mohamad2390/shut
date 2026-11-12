package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.pricing.PricingService;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/** The price widget asks for a page of prices: {@code "i"} for the first, {@code "p:3"} for the third. */
@Component
@TelegramBotEnabled
class PriceWidgetAnswerer implements WidgetAnswerer {

    private static final String PAGE_PREFIX = "p:";

    private final PricingService pricing;

    PriceWidgetAnswerer(PricingService pricing) {
        this.pricing = pricing;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.PRICES;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return PriceWidgetState.page(PriceWidgetPresenter.heading(WidgetNames.of(provider)),
                pricing.list(provider.userId()), requestedPage(request));
    }

    /** The first page unless the widget asked for another one. */
    static int requestedPage(String request) {
        if (request != null && request.startsWith(PAGE_PREFIX)) {
            try {
                return Integer.parseInt(request.substring(PAGE_PREFIX.length()));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}
