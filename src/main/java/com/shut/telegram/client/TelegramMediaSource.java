package com.shut.telegram.client;

import com.shut.api.media.MediaFile;
import com.shut.api.media.MediaSource;
import com.shut.api.media.MediaUnavailableException;
import com.shut.api.media.MediaTypes;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import com.shut.telegram.model.ApiResponse;
import com.shut.telegram.model.GetFileRequest;
import com.shut.telegram.model.TelegramFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/**
 * Fetches media from Telegram for the web app. The download URL contains the bot token, so it never leaves the server:
 * the browser gets the bytes from our own endpoint.
 */
@Component
@TelegramBotEnabled
class TelegramMediaSource implements MediaSource {

    /** The Bot API refuses to download anything bigger than this. */
    static final long MAX_BYTES = 20L * 1024 * 1024;

    private final RestClient api;
    private final RestClient downloads;
    private final String fileBaseUrl;
    private final String token;

    TelegramMediaSource(RestClient telegramRestClient, RestClient.Builder builder, TelegramProperties properties) {
        this.api = telegramRestClient;
        this.downloads = builder.build();
        this.token = properties.token();
        this.fileBaseUrl = properties.apiBaseUrl() + "/file/bot" + properties.token() + "/";
    }

    @Override
    public MediaFile fetch(String fileId) {
        try {
            ApiResponse<TelegramFile> response = api.post()
                    .uri("/getFile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GetFileRequest(fileId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !response.ok() || response.result() == null
                    || response.result().filePath() == null) {
                throw new MediaUnavailableException("Telegram has no file for this id");
            }
            TelegramFile file = response.result();
            if (file.fileSize() != null && file.fileSize() > MAX_BYTES) {
                throw new MediaUnavailableException("The file is too big to download");
            }
            byte[] content = downloads.get().uri(URI.create(fileBaseUrl + file.filePath())).retrieve().body(byte[].class);
            if (content == null || content.length == 0) {
                throw new MediaUnavailableException("Telegram sent an empty file");
            }
            return new MediaFile(content, MediaTypes.of(file.filePath()));
        } catch (RestClientException e) {
            throw new MediaUnavailableException("Telegram download failed: " + scrub(e.getMessage()));
        }
    }

    private String scrub(String message) {
        return message == null ? "no details" : message.replace(token, "***");
    }
}
