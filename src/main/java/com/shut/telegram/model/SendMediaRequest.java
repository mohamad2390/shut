package com.shut.telegram.model;

/** Sends a photo, video or document by file id. Exactly one of the three file fields is set. */
@TelegramModel
public record SendMediaRequest(
        long chatId,
        String photo,
        String video,
        String document,
        String caption,
        InlineKeyboardMarkup replyMarkup
) {
    public static SendMediaRequest photo(long chatId, String fileId, String caption, InlineKeyboardMarkup markup) {
        return new SendMediaRequest(chatId, fileId, null, null, caption, markup);
    }

    public static SendMediaRequest video(long chatId, String fileId, String caption, InlineKeyboardMarkup markup) {
        return new SendMediaRequest(chatId, null, fileId, null, caption, markup);
    }

    public static SendMediaRequest document(long chatId, String fileId, String caption, InlineKeyboardMarkup markup) {
        return new SendMediaRequest(chatId, null, null, fileId, caption, markup);
    }

    /** The Bot API method that matches the file field that is set. */
    public String apiMethod() {
        if (photo != null) {
            return "sendPhoto";
        }
        return video != null ? "sendVideo" : "sendDocument";
    }
}
