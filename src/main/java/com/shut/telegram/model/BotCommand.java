package com.shut.telegram.model;

/** One entry of the bot's command menu. The command is lower case latin; the description is what the user reads. */
@TelegramModel
public record BotCommand(String command, String description) {
}
