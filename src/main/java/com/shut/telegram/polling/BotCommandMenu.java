package com.shut.telegram.polling;

import com.shut.telegram.model.BotCommand;

import java.util.List;

/** The two entries of the menu next to the attach icon in the chat. */
final class BotCommandMenu {

    static final List<BotCommand> COMMANDS = List.of(
            new BotCommand("clear", "🧹 پاک‌سازی چت و برگشت به منوی اصلی"),
            new BotCommand("menu", "🏠 برگشت به منوی اصلی"));

    private BotCommandMenu() {
    }
}
