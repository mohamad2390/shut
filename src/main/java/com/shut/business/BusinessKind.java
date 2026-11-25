package com.shut.business;

import com.shut.user.entity.RoleCodes;

import java.util.Optional;

/** The business categories that are a basic card (name, city, description); the others have their own sections. */
public enum BusinessKind {
    RENTAL("rental", RoleCodes.EQUIPMENT_RENTER, "🎒", "اجارهٔ تجهیزات");

    private final String key;
    private final String roleCode;
    private final String icon;
    private final String title;

    BusinessKind(String key, String roleCode, String icon, String title) {
        this.key = key;
        this.roleCode = roleCode;
        this.icon = icon;
        this.title = title;
    }

    public String key() { return key; }
    public String roleCode() { return roleCode; }
    public String icon() { return icon; }
    public String title() { return title; }

    public static Optional<BusinessKind> fromKey(String key) {
        for (BusinessKind kind : values()) {
            if (kind.key.equals(key)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
