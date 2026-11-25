package com.shut.api.me.venue;

/** Who asked for a venue, so the owner can answer them. */
public record RequesterResponse(String name, String telegramUsername, String phone) {
}
