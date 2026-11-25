package com.shut.api.market;

/** How to reach someone: their name, Telegram username when they have one, and their phone number. */
public record ContactResponse(String name, String telegramUsername, String phone) {
}
