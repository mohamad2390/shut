package com.shut.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What members can pick from when adding a price: the services and the icon of every unit.
 * Both live in the database, so they can be changed without a new release.
 */
@Service
public class PricingCatalog {

    /** Shown for a price that was typed by hand and so has no service. */
    public static final String DEFAULT_SERVICE_ICON = "🏷";
    /** Shown for a unit that has no row in {@code pricing_unit_icons}. */
    public static final String DEFAULT_UNIT_ICON = "💰";

    private final ServiceTypeRepository services;
    private final UnitIconRepository unitIcons;

    public PricingCatalog(ServiceTypeRepository services, UnitIconRepository unitIcons) {
        this.services = services;
        this.unitIcons = unitIcons;
    }

    /** The services on offer, in the order they should be listed. */
    @Transactional(readOnly = true)
    public List<ServiceType> services() {
        return services.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    /** A service that is still on offer. */
    @Transactional(readOnly = true)
    public Optional<ServiceType> service(long id) {
        return services.findById(id).filter(ServiceType::isActive);
    }

    /** The icon of every unit; a unit without one gets the default icon. */
    @Transactional(readOnly = true)
    public Map<PricingUnit, String> unitIcons() {
        Map<PricingUnit, String> icons = new EnumMap<>(PricingUnit.class);
        for (PricingUnit unit : PricingUnit.values()) {
            icons.put(unit, DEFAULT_UNIT_ICON);
        }
        unitIcons.findAll().forEach(row -> row.getUnit().ifPresent(unit -> icons.put(unit, row.getIcon())));
        return icons;
    }
}
