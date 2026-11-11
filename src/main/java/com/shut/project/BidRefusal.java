package com.shut.project;

/** Why an offer cannot be placed. */
public enum BidRefusal {
    PROJECT_NOT_FOUND,
    /** The project was awarded or cancelled, or its deadline passed. */
    NOT_TAKING_BIDS,
    /** Members cannot bid on their own projects. */
    OWN_PROJECT
}
