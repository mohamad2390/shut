package com.shut.telegram.widget;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.model.SendWidgetRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WidgetDeliveryTest {

    private static final long CHAT = 500L;
    private static final long PROVIDER = 7L;
    private static final WidgetKind KIND = WidgetKind.CALENDAR;

    private final TelegramApiClient telegram = mock(TelegramApiClient.class);
    private final WidgetMessageRepository widgets = mock(WidgetMessageRepository.class);
    private final WidgetDelivery delivery = new WidgetDelivery(telegram, widgets);

    private static WidgetMessage earlier(long messageId) {
        return new WidgetMessage(CHAT, messageId, PROVIDER, KIND);
    }

    @Test
    void sendsTheWidgetAloneAndRemembersWhatItIsAbout() {
        when(telegram.sendWidget(any())).thenReturn(99L);

        assertThat(delivery.replace(CHAT, PROVIDER, KIND, "<html>", "{\"h\":1}")).isTrue();

        ArgumentCaptor<SendWidgetRequest> sent = ArgumentCaptor.forClass(SendWidgetRequest.class);
        verify(telegram).sendWidget(sent.capture());
        assertThat(sent.getValue().chatId()).isEqualTo(CHAT);
        assertThat(sent.getValue().html()).isEqualTo("<html>");
        assertThat(sent.getValue().chatData()).isEqualTo("{\"h\":1}");
        assertThat(sent.getValue().allowQueries()).isTrue();
        assertThat(sent.getValue().caption()).isNull();
        ArgumentCaptor<WidgetMessage> saved = ArgumentCaptor.forClass(WidgetMessage.class);
        verify(widgets).save(saved.capture());
        assertThat(saved.getValue().getMessageId()).isEqualTo(99L);
        assertThat(saved.getValue().getProviderId()).isEqualTo(PROVIDER);
        assertThat(saved.getValue().getKind()).isEqualTo(KIND);
    }

    @Test
    void askingAgainDeletesTheEarlierWidgetsOfTheSameKindAndMember() {
        when(widgets.findByChatIdAndProviderIdAndKind(CHAT, PROVIDER, KIND)).thenReturn(List.of(earlier(41L), earlier(42L)));
        when(telegram.sendWidget(any())).thenReturn(99L);

        delivery.replace(CHAT, PROVIDER, KIND, "<html>", "{}");

        verify(telegram).deleteMessage(Long.toString(CHAT), 41L);
        verify(telegram).deleteMessage(Long.toString(CHAT), 42L);
        verify(widgets, times(2)).delete(any(WidgetMessage.class));
    }

    @Test
    void anEarlierWidgetAlreadyDeletedDoesNotStopTheNewOne() {
        WidgetMessage gone = earlier(41L);
        when(widgets.findByChatIdAndProviderIdAndKind(CHAT, PROVIDER, KIND)).thenReturn(List.of(gone));
        when(telegram.sendWidget(any())).thenReturn(99L);
        doThrow(new TelegramApiException("message to delete not found")).when(telegram).deleteMessage(any(), anyLong());

        assertThat(delivery.replace(CHAT, PROVIDER, KIND, "<html>", "{}")).isTrue();
        verify(widgets).delete(gone);
    }

    @Test
    void theEarlierWidgetStaysWhenTheNewOneCannotBeSent() {
        when(widgets.findByChatIdAndProviderIdAndKind(CHAT, PROVIDER, KIND)).thenReturn(List.of(earlier(41L)));
        when(telegram.sendWidget(any())).thenThrow(new TelegramApiException("widgets are not supported here"));

        assertThat(delivery.replace(CHAT, PROVIDER, KIND, "<html>", "{}")).isFalse();

        verify(telegram, never()).deleteMessage(any(), anyLong());
        verify(widgets, never()).delete(any(WidgetMessage.class));
        verify(widgets, never()).save(any());
    }
}
