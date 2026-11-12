package com.shut.telegram.widget;

import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceWidgetPresenterTest {

    private static final long CHAT = 500L;
    private static final long PROVIDER = 7L;

    private final WidgetDelivery delivery = mock(WidgetDelivery.class);
    private final PriceWidgetPresenter presenter = new PriceWidgetPresenter(delivery, new WidgetHtml());

    private static List<PriceItem> items() {
        PriceItem item = new PriceItem(PROVIDER, "عکاسی", PricingUnit.HOUR, 1_000_000);
        ReflectionTestUtils.setField(item, "id", 1L);
        return List.of(item);
    }

    @Test
    void sendsThePriceWidgetWithTheFirstPage() {
        when(delivery.replace(anyLong(), anyLong(), any(), any(), any())).thenReturn(true);

        assertThat(presenter.show(CHAT, PROVIDER, "علی", items())).isTrue();

        ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
        verify(delivery).replace(eq(CHAT), eq(PROVIDER), eq(WidgetKind.PRICES), any(), state.capture());
        assertThat(state.getValue()).contains("قیمت‌های علی", "عکاسی");
    }

    @Test
    void reportsFailureSoThePricesCanBeShownAsText() {
        when(delivery.replace(anyLong(), anyLong(), any(), any(), any())).thenReturn(false);

        assertThat(presenter.show(CHAT, PROVIDER, "علی", items())).isFalse();
    }
}
