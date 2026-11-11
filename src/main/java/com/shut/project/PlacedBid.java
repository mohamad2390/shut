package com.shut.project;

/** A saved offer. {@code revised} is true when it replaced an earlier offer from the same provider. */
public record PlacedBid(Bid bid, Project project, boolean revised) {
}
