package com.shut.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * One product: a type, a brand and a model name together, such as camera + Sony + Alpha 7.
 * The same model name under another brand or type is another product.
 */
@Entity
@Table(name = "equipment_models")
public class EquipmentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "type_id", nullable = false, updatable = false)
    private EquipmentType type;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "brand_id", nullable = false, updatable = false)
    private EquipmentBrand brand;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    /** False for what a member added, until someone has looked at it. */
    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EquipmentModel() {
    }

    /** A product a member added. */
    EquipmentModel(EquipmentType type, EquipmentBrand brand, String name, Long createdByUserId) {
        this.type = type;
        this.brand = brand;
        this.name = EquipmentNames.display(name);
        this.normalizedName = EquipmentNames.key(name);
        this.createdByUserId = createdByUserId;
        this.createdAt = OffsetDateTime.now();
    }

    /** The brand and model together, such as "سونی آلفا ۷". */
    public String displayName() {
        return brand.getName() + " " + name;
    }

    public Long getId() { return id; }
    public EquipmentType getType() { return type; }
    public EquipmentBrand getBrand() { return brand; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public boolean isVerified() { return verified; }
    public Long getCreatedByUserId() { return createdByUserId; }
}
