package com.shut.project;

import java.util.List;

/** The outcome of choosing an offer: the project, the offer that won and the offers that did not. */
public record Award(Project project, Bid winner, List<Bid> rejected) {
}
