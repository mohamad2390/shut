package com.shut.api.showcase;

/** One portfolio item. The picture or video is fetched by id from {@code .../showcase/{id}/media}. */
public record ShowcaseItemResponse(Long id, String mediaType, String caption) {
}
