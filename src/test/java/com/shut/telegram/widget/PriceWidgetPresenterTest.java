package com.shut.telegram.widget;

import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingUnit;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.model.SendWidgetRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceWidgetPresenterTest {

    private static final long CHAT = 500L;
    private static final long PROVIDER = 7L;

    private final TelegramApiClient telegram = mock(TelegramApiClient.class);
    private final PriceWidgetRepository widgets = mock(PriceWidgetRepository.class);
    private final PriceWidgetPresenter presenter = new PriceWidgetPresenter(telegram, new WidgetHtml(), widgets);

    private static List<PriceItem> items() {
        PriceItem item = new PriceItem(PROVIDER, "عکاسی", PricingUnit.HOUR, 1_000_000);
        ReflectionTestUtils.setField(item, "id", 1L);
        return List.of(item);
    }

    private static PriceWidget earlierWidget(long messageId) {
        return new PriceWidget(CHAT, messageId, PROVIDER);
    }

    @Test
    void sendsTheWidgetWithTheFirstPageAndRemembersWhoseItIs() {
        when(telegram.sendWidget(any())).thenReturn(99L);

        boolean shown = presenter.show(CHAT, PROVIDER, "علی", items());

        assertThat(shown).isTrue();
        ArgumentCaptor<SendWidgetRequest> sent = ArgumentCaptor.forClass(SendWidgetRequest.class);
        verify(telegram).sendWidget(sent.capture());
        assertThat(sent.getValue().chatId()).isEqualTo(CHAT);
        assertThat(sent.getValue().allowQueries()).isTrue();
        assertThat(sent.getValue().chatData()).contains("قیمت‌های علی", "عکاسی");
        ArgumentCaptor<PriceWidget> saved = ArgumentCaptor.forClass(PriceWidget.class);
        verify(widgets).save(saved.capture());
        assertThat(saved.getValue().getMessageId()).isEqualTo(99L);
        assertThat(saved.getValue().getProviderId()).isEqualTo(PROVIDER);
    }

    @Test
    void askingAgainDeletesTheEarlierWidgetOfTheSameMember() {
        when(widgets.findByChatIdAndProviderId(CHAT, PROVIDER)).thenReturn(List.of(earlierWidget(41L), earlierWidget(42L)));
        when(telegram.sendWidget(any())).thenReturn(99L);

        presenter.show(CHAT, PROVIDER, "علی", items());

        verify(telegram).deleteMessage(Long.toString(CHAT), 41L);
        verify(telegram).deleteMessage(Long.toString(CHAT), 42L);
        verify(widgets, org.mockito.Mockito.times(2)).delete(any(PriceWidget.class));
    }

    @Test
    void anEarlierWidgetTheMemberAlreadyDeletedDoesNotStopTheNewOne() {
        PriceWidget earlier = earlierWidget(41L);
        when(widgets.findByChatIdAndProviderId(CHAT, PROVIDER)).thenReturn(List.of(earlier));
        when(telegram.sendWidget(any())).thenReturn(99L);
        doThrow(new TelegramApiException("message to delete not found")).when(telegram).deleteMessage(any(), org.mockito.ArgumentMatchers.anyLong());

        assertThat(presenter.show(CHAT, PROVIDER, "علی", items())).isTrue();
        verify(widgets).delete(earlier);
    }

    @Test
    void theEarlierWidgetStaysWhenTheNewOneCannotBeSent() {
        when(widgets.findByChatIdAndProviderId(CHAT, PROVIDER)).thenReturn(List.of(earlierWidget(41L)));
        when(telegram.sendWidget(any())).thenThrow(new TelegramApiException("widgets are not supported here"));

        assertThat(presenter.show(CHAT, PROVIDER, "علی", items())).isFalse();

        verify(telegram, never()).deleteMessage(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(widgets, never()).delete(any(PriceWidget.class));
        verify(widgets, never()).save(any());
    }
}
