package com.shut.api.studio;

import java.util.List;

/** A package a studio sells. {@code price} is null when it is "call us"; {@code services} are catalog labels. */
public record StudioOfferingResponse(Long id, String title, List<String> services, Long price) {
}
