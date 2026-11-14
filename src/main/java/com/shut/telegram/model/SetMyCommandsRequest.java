package com.shut.telegram.model;

import java.util.List;

@TelegramModel
public record SetMyCommandsRequest(List<BotCommand> commands) {
}
