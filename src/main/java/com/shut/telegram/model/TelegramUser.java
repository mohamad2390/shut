package com.shut.telegram.model;

import com.fasterxml.jackson.annotation.JsonProperty;

@TelegramModel
public record TelegramUser(
        long id,
        @JsonProperty("is_bot") boolean bot,
        String firstName,
        String lastName,
        String username
) {
}
