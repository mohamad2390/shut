package com.shut.api;

/** Keeps the page and size a client asks for within sane limits. */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private Paging() {
    }

    public static int page(int requested) {
        return Math.max(0, requested);
    }

    public static int size(int requested) {
        if (requested <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(requested, MAX_SIZE);
    }
}
