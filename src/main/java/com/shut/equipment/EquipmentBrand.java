package com.shut.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A maker of gear, such as Sony. One brand makes gear of many types. */
@Entity
@Table(name = "equipment_brands")
public class EquipmentBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 80)
    private String normalizedName;

    /** False for what a member added, until someone has looked at it. */
    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EquipmentBrand() {
    }

    /** A brand a member added. */
    EquipmentBrand(String name, Long createdByUserId) {
        this.name = EquipmentNames.display(name);
        this.normalizedName = EquipmentNames.key(name);
        this.createdByUserId = createdByUserId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public boolean isVerified() { return verified; }
    public Long getCreatedByUserId() { return createdByUserId; }
}
