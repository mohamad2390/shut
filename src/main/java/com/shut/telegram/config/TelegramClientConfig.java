package com.shut.telegram.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Builds the HTTP client used to talk to the Telegram Bot API. */
@Configuration
@TelegramBotEnabled
@EnableConfigurationProperties(TelegramProperties.class)
class TelegramClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Extra read time on top of the long-poll timeout, so Telegram answers before we give up. */
    private static final int READ_TIMEOUT_MARGIN_SECONDS = 15;

    @Bean
    RestClient telegramRestClient(RestClient.Builder builder, TelegramProperties properties) {
        if (properties.token() == null || properties.token().isBlank()) {
            throw new IllegalStateException(
                    "telegram.bot.token (TELEGRAM_BOT_TOKEN) must be set when telegram.bot.enabled=true");
        }

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(
                Duration.ofSeconds(properties.pollTimeoutSeconds() + READ_TIMEOUT_MARGIN_SECONDS));

        return builder
                .baseUrl(properties.apiBaseUrl() + "/bot" + properties.token())
                .requestFactory(requestFactory)
                .build();
    }
}
