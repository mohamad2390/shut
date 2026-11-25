package com.shut.api.media;

import java.util.Locale;
import java.util.Map;

/**
 * The content type of a stored file, from its extension. Only pictures and videos get their real type; anything else
 * (an uploaded document) is sent as plain bytes so a browser never runs it.
 */
public final class MediaTypes {

    private static final Map<String, String> BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "mp4", "video/mp4",
            "mov", "video/quicktime");
    public static final String UNKNOWN = "application/octet-stream";

    private MediaTypes() {
    }

    public static String of(String filePath) {
        int dot = filePath == null ? -1 : filePath.lastIndexOf('.');
        if (dot < 0) {
            return UNKNOWN;
        }
        return BY_EXTENSION.getOrDefault(filePath.substring(dot + 1).toLowerCase(Locale.ROOT), UNKNOWN);
    }
}
