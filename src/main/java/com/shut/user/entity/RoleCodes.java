package com.shut.user.entity;

/** The codes of the roles the application itself checks for. Other roles can be added in the database. */
public final class RoleCodes {

    /** An ordinary user. */
    public static final String CUSTOMER = "CUSTOMER";
    /** A specialist: someone with a profile who offers services. */
    public static final String PROVIDER = "PROVIDER";
    public static final String ADMIN = "ADMIN";

    private RoleCodes() {
    }
}
