package com.shut.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A service a member can put on their price menu, such as "wedding photography", with the icon shown next to it. */
@Entity
@Table(name = "service_types")
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(nullable = false, length = 16)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Services that are not active are no longer offered, but prices already added keep showing. */
    @Column(nullable = false)
    private boolean active = true;

    /** Whether the member may give a price of this service a name of their own (used for "other services"). */
    @Column(name = "asks_name", nullable = false)
    private boolean asksName;

    protected ServiceType() {
    }

    public ServiceType(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public boolean asksName() { return asksName; }
}
