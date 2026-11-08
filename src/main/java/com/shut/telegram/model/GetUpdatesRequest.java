package com.shut.telegram.model;

import java.util.List;

@TelegramModel
public record GetUpdatesRequest(long offset, int timeout, List<String> allowedUpdates) {
}
