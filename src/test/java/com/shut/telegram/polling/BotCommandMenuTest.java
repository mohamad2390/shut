package com.shut.telegram.polling;

import com.shut.telegram.model.BotCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotCommandMenuTest {

    @Test
    void hasTheCleanAndTheMenuCommandsInThatOrder() {
        assertThat(BotCommandMenu.COMMANDS).extracting(BotCommand::command).containsExactly("clear", "menu");
    }

    @Test
    void commandNamesAreLowerCaseLatinAsTelegramRequires() {
        assertThat(BotCommandMenu.COMMANDS).allSatisfy(command ->
                assertThat(command.command()).matches("[a-z0-9_]{1,32}"));
    }
}
