package com.shut.telegram.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs, but only while the bot runs. */
@Configuration
@EnableScheduling
@TelegramBotEnabled
class BotSchedulingConfig {
}
