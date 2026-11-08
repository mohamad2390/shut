package com.shut.telegram.model;

@TelegramModel
public record Chat(long id, String type) {

    public boolean isPrivate() {
        return "private".equals(type);
    }
}
