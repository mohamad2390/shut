package com.shut.telegram.cleanup;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.widget.WidgetMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatCleanerTest {

    private static final long CHAT = 500L;

    private final TelegramApiClient telegram = mock(TelegramApiClient.class);
    private final ChatMessageLog messages = mock(ChatMessageLog.class);
    private final WidgetMessageRepository widgets = mock(WidgetMessageRepository.class);
    private final ChatCleaner cleaner = new ChatCleaner(telegram, messages, widgets);

    @Test
    void deletesEveryRememberedMessageInBatchesOfAHundred() {
        List<Long> ids = LongStream.rangeClosed(1, 250).boxed().toList();
        when(messages.messageIds(CHAT)).thenReturn(ids);

        cleaner.clear(CHAT);

        ArgumentCaptor<List<Long>> batches = ArgumentCaptor.forClass(List.class);
        verify(telegram, times(3)).deleteMessages(eq("500"), batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(100, 100, 50);
        verify(messages).forget(CHAT);
        verify(widgets).deleteByChatId(CHAT);
    }

    @Test
    void aFailedBatchDoesNotStopTheRestOrTheForgetting() {
        when(messages.messageIds(CHAT)).thenReturn(LongStream.rangeClosed(1, 150).boxed().toList());
        doThrow(new TelegramApiException("boom")).when(telegram).deleteMessages(any(), anyList());

        cleaner.clear(CHAT);

        verify(telegram, times(2)).deleteMessages(any(), anyList());
        verify(messages).forget(CHAT);
        verify(widgets).deleteByChatId(CHAT);
    }

    @Test
    void anEmptyChatCallsNothingOnTelegram() {
        when(messages.messageIds(CHAT)).thenReturn(List.of());

        cleaner.clear(CHAT);

        verify(telegram, never()).deleteMessages(any(), anyList());
        verify(messages).forget(CHAT);
    }
}
