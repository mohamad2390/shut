package com.shut.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed binding for the {@code telegram.bot.*} section of application.yml.
 * {@code channelId} ("@name" or numeric id) turns on the membership check; blank disables it.
 */
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties(
        boolean enabled,
        String token,
        @DefaultValue("https://api.telegram.org") String apiBaseUrl,
        @DefaultValue("30") int pollTimeoutSeconds,
        @DefaultValue("5") int retryDelaySeconds,
        String channelId,
        String channelLink
) {
}
