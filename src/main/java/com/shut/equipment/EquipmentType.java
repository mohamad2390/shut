package com.shut.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A kind of gear, such as "camera" or "lens", with the icon the bot shows next to it. */
@Entity
@Table(name = "equipment_types")
public class EquipmentType {

    /** Types members add themselves go after the ones we listed. */
    static final int ADDED_BY_MEMBER_SORT_ORDER = 1000;
    static final String ADDED_BY_MEMBER_ICON = "🔧";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 80)
    private String normalizedName;

    @Column(nullable = false, length = 16)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** False for what a member added, until someone has looked at it. */
    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EquipmentType() {
    }

    /** A type a member added. */
    EquipmentType(String name, Long createdByUserId) {
        this.name = EquipmentNames.display(name);
        this.normalizedName = EquipmentNames.key(name);
        this.icon = ADDED_BY_MEMBER_ICON;
        this.sortOrder = ADDED_BY_MEMBER_SORT_ORDER;
        this.createdByUserId = createdByUserId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getIcon() { return icon; }
    public int getSortOrder() { return sortOrder; }
    public boolean isVerified() { return verified; }
    public Long getCreatedByUserId() { return createdByUserId; }
}
