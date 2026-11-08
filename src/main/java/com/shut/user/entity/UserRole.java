package com.shut.user.entity;

/** Roles are additive: one account may be both a customer and a service provider. */
public enum UserRole {
    CUSTOMER,
    PROVIDER,
    ADMIN
}
