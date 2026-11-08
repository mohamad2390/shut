package com.shut.telegram.model;

/** Envelope that wraps every Bot API response. */
@TelegramModel
public record ApiResponse<T>(boolean ok, T result, String description, Integer errorCode) {
}
