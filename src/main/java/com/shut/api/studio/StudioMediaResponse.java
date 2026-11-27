package com.shut.api.studio;

/** A sample of the studio's work, {@code PHOTO} or {@code VIDEO}. The file itself is fetched separately by id. */
public record StudioMediaResponse(Long id, String kind) {
}
