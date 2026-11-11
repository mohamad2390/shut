package com.shut.project;

/** The member already has as many open projects as are allowed. */
public class ProjectLimitReachedException extends RuntimeException {

    public ProjectLimitReachedException() {
        super("Too many open projects");
    }
}
