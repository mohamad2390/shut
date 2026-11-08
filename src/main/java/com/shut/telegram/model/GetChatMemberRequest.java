package com.shut.telegram.model;

@TelegramModel
public record GetChatMemberRequest(String chatId, long userId) {
}
