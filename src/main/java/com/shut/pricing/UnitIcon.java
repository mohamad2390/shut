package com.shut.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Optional;

/** The icon shown for a pricing unit. */
@Entity
@Table(name = "pricing_unit_icons")
public class UnitIcon {

    /** The name of a {@link PricingUnit}. */
    @Id
    @Column(length = 20)
    private String unit;

    @Column(nullable = false, length = 16)
    private String icon;

    protected UnitIcon() {
    }

    public UnitIcon(PricingUnit unit, String icon) {
        this.unit = unit.name();
        this.icon = icon;
    }

    /** The unit this row is for, or empty if the row names a unit that does not exist (any more). */
    public Optional<PricingUnit> getUnit() { return PricingUnit.fromKey(unit); }
    public String getIcon() { return icon; }
}
