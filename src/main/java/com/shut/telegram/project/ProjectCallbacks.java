package com.shut.telegram.project;

import com.shut.showcase.ShowcaseCategory;

/** Callback data for projects. Format: {@code project:<action>[:<argument>...]}. */
public final class ProjectCallbacks {

    public static final String PREFIX = "project";
    /** Opens the projects section; used by the main menu. */
    public static final String HOME = PREFIX + ":home";
    static final String NEW = PREFIX + ":new";
    /** Leaves the project being created. */
    static final String ABANDON = PREFIX + ":abandon";
    static final String SKIP_CITY = PREFIX + ":skipcity";
    static final String SKIP_BUDGET = PREFIX + ":skipbudget";
    /** The filter value for "every category". */
    static final String ALL = "ALL";

    private ProjectCallbacks() {
    }

    static String category(ShowcaseCategory category) {
        return PREFIX + ":cat:" + category.name();
    }

    static String deadline(int days) {
        return PREFIX + ":dl:" + days;
    }

    /** Open projects, filtered by a category name or {@link #ALL}. */
    static String open(String filter, int page) {
        return PREFIX + ":open:" + filter + ":" + page;
    }

    static String mine(int page) {
        return PREFIX + ":mine:" + page;
    }

    static String view(long projectId) {
        return PREFIX + ":view:" + projectId;
    }

    static String cancel(long projectId) {
        return PREFIX + ":cancel:" + projectId;
    }

    static String confirmCancel(long projectId) {
        return PREFIX + ":cancelok:" + projectId;
    }
}
