package com.shut.telegram.model;

/** A contact the user shared. {@code userId} is the Telegram account it belongs to, which is how we know it is theirs. */
@TelegramModel
public record Contact(String phoneNumber, String firstName, Long userId) {
}
