package com.shut.user.entity;

/** The codes of the roles the application itself checks for. Other roles can be added in the database. */
public final class RoleCodes {

    /** An ordinary user. */
    public static final String CUSTOMER = "CUSTOMER";
    /** A specialist: someone with a profile who offers services. */
    public static final String PROVIDER = "PROVIDER";
    /** A studio owner: a team and a studio place, with any of the studio abilities. */
    public static final String STUDIO_OWNER = "STUDIO_OWNER";
    /** Someone who rents out a location for shoots. */
    public static final String VENUE_OWNER = "VENUE_OWNER";
    public static final String EQUIPMENT_RENTER = "EQUIPMENT_RENTER";
    public static final String ADMIN = "ADMIN";

    private RoleCodes() {
    }
}
