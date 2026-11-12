package com.shut.telegram.widget;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

/**
 * What Telegram accepts as a widget page: at most 32768 bytes, at most 4096 bytes after zlib compression at level 9,
 * and no NUL character.
 */
final class WidgetLimits {

    static final int MAX_BYTES = 32768;
    static final int MAX_COMPRESSED_BYTES = 4096;

    private WidgetLimits() {
    }

    /** What Telegram would refuse about this page, or null if nothing. */
    static String problemWith(String html) {
        if (html.indexOf('\u0000') >= 0) {
            return "it contains a NUL character";
        }
        int bytes = html.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BYTES) {
            return "it is " + bytes + " bytes, the limit is " + MAX_BYTES;
        }
        int compressed = compressedSize(html);
        if (compressed > MAX_COMPRESSED_BYTES) {
            return "it is " + compressed + " bytes compressed, the limit is " + MAX_COMPRESSED_BYTES;
        }
        return null;
    }

    static int compressedSize(String html) {
        Deflater deflater = new Deflater(9);
        try {
            deflater.setInput(html.getBytes(StandardCharsets.UTF_8));
            deflater.finish();
            byte[] buffer = new byte[8192];
            int total = 0;
            while (!deflater.finished()) {
                total += deflater.deflate(buffer);
            }
            return total;
        } finally {
            deflater.end();
        }
    }
}
