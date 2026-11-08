package com.shut.telegram.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A user's status in a chat. {@code joined} is only set for "restricted" members. */
@TelegramModel
public record ChatMember(String status, @JsonProperty("is_member") Boolean joined) {
}
