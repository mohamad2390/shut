package com.shut.api.studio;

/** A specialist who accepted to be listed in the studio. {@code userId} is also their provider page id. */
public record StudioPersonResponse(Long userId, String name) {
}
