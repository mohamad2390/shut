package com.shut.project;

/** Where a project is in its life. Whether it still takes offers also depends on its deadline. */
public enum ProjectStatus {
    /** Waiting for offers, or for the owner to choose one. */
    OPEN,
    /** The owner chose an offer. */
    AWARDED,
    /** The owner withdrew the project. */
    CANCELLED
}
