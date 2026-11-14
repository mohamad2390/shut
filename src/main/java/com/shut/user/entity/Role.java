package com.shut.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * What an account is allowed to be, such as a customer or a provider. Roles are rows of the {@code roles} table,
 * so a new role needs no new code; the code only has to know the ones it checks for, see {@link RoleCodes}.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What the code refers to the role by, for example {@code PROVIDER}. It never changes. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** What people see, for example "متخصص". */
    @Column(nullable = false, length = 60)
    private String title;

    /** Every new account gets the default roles. */
    @Column(name = "is_default", nullable = false)
    private boolean defaultRole;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Role() {
    }

    public Role(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public boolean isDefaultRole() { return defaultRole; }
    public int getSortOrder() { return sortOrder; }

    /** Two roles are the same role if they have the same code. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Role role && Objects.equals(code, role.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
