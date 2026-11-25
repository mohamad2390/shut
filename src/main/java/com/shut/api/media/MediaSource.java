package com.shut.api.media;

/** Where the media itself lives. Today that is Telegram; an own storage server can replace it later. */
public interface MediaSource {

    /**
     * @throws MediaUnavailableException if the file cannot be fetched (deleted, too big for the Bot API, or the
     *                                   storage is down)
     */
    MediaFile fetch(String fileId);
}
