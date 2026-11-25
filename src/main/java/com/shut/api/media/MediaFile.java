package com.shut.api.media;

/** The bytes of a stored picture or video and what kind of file it is. */
public record MediaFile(byte[] content, String contentType) {
}
