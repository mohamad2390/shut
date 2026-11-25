package com.shut.api.showcase;

import com.shut.api.venue.LabelResponse;

/** A portfolio section that has at least one item. */
public record ShowcaseCategoryResponse(LabelResponse category, long count) {
}
