package com.shut.telegram.messaging;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.model.TelegramUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotLinksTest {

    @Test
    void readsTheMemberIdFromAStartText() {
        assertThat(BotLinks.profileIdIn("/start p42").getAsLong()).isEqualTo(42);
        assertThat(BotLinks.profileIdIn("/start@shut_bot p7").getAsLong()).isEqualTo(7);
    }

    @Test
    void otherStartTextsAreNotProfileLinks() {
        assertThat(BotLinks.profileIdIn("/start")).isEmpty();
        assertThat(BotLinks.profileIdIn("/start x42")).isEmpty();
        assertThat(BotLinks.profileIdIn("/start p")).isEmpty();
        assertThat(BotLinks.profileIdIn("/start p4 2")).isEmpty();
        assertThat(BotLinks.profileIdIn(null)).isEmpty();
    }

    @Test
    void theLinkOpensTheBotOnTheMember() {
        TelegramApiClient telegram = mock(TelegramApiClient.class);
        when(telegram.getMe()).thenReturn(new TelegramUser(1L, true, "SHUT", null, "shut_bot"));

        assertThat(new BotLinks(telegram).profileLink(42)).contains("https://t.me/shut_bot?start=p42");
    }

    @Test
    void noLinkWhenTheBotCannotBeAsked() {
        TelegramApiClient telegram = mock(TelegramApiClient.class);
        when(telegram.getMe()).thenThrow(new IllegalStateException("down"));

        assertThat(new BotLinks(telegram).profileLink(42)).isEmpty();
    }

    @Test
    void theShareButtonEncodesTheLinkAndText() {
        String url = BotLinks.shareButton("https://t.me/shut_bot?start=p42", "پروفایل من").url();

        assertThat(url).startsWith("https://t.me/share/url?url=https%3A%2F%2Ft.me%2Fshut_bot%3Fstart%3Dp42&text=");
        assertThat(url).doesNotContain(" ");
    }
}
