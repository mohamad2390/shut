package com.shut.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Another spelling of a brand, such as "Sony" for "سونی". Added in the database, never by members. */
@Entity
@Table(name = "equipment_brand_aliases")
public class EquipmentBrandAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(nullable = false, length = 80)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, unique = true, length = 80)
    private String normalizedAlias;

    protected EquipmentBrandAlias() {
    }

    public Long getBrandId() { return brandId; }
    public String getAlias() { return alias; }
}
