package com.shut.api.studio;

/** One studio in the list. */
public record StudioListItem(Long id, String name, String city, long offeringCount, long mediaCount) {
}
