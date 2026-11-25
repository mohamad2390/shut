package com.shut.api.admin;

import com.shut.api.BadRequestException;
import com.shut.api.ConflictException;
import com.shut.api.NotFoundException;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingUnit;
import com.shut.pricing.ServiceType;
import com.shut.pricing.ServiceTypeRepository;
import com.shut.pricing.UnitIcon;
import com.shut.pricing.UnitIconRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Editing the lists members pick from when they write a price: the services and the icon of each unit. A service is
 * never deleted, only switched off, so the prices that already use it keep showing.
 */
@Service
public class AdminCatalogApiService {

    static final int MAX_NAME = 60;
    static final int MAX_ICON = 16;
    static final int MAX_SORT_ORDER = 10_000;

    private final ServiceTypeRepository services;
    private final UnitIconRepository unitIcons;
    private final PricingCatalog catalog;

    public AdminCatalogApiService(ServiceTypeRepository services, UnitIconRepository unitIcons,
                                  PricingCatalog catalog) {
        this.services = services;
        this.unitIcons = unitIcons;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<AdminServiceItem> services() {
        return services.findAllByOrderBySortOrderAscIdAsc().stream().map(AdminServiceItem::of).toList();
    }

    /**
     * @throws BadRequestException if the name or icon is missing or too long
     * @throws ConflictException   if another service has the name
     */
    @Transactional
    public AdminServiceItem create(ServiceRequest request) {
        String name = name(request.name());
        String icon = icon(request.icon());
        if (services.findByName(name).isPresent()) {
            throw new ConflictException("SERVICE_NAME_TAKEN", "There is a service with this name already");
        }
        ServiceType service = new ServiceType(name, icon);
        applyOptional(service, request);
        return AdminServiceItem.of(services.save(service));
    }

    /**
     * @throws NotFoundException   if there is no such service
     * @throws BadRequestException if a value is not valid
     * @throws ConflictException   if another service has the name
     */
    @Transactional
    public AdminServiceItem update(Long id, ServiceRequest request) {
        ServiceType service = services.findById(id).orElseThrow(() -> new NotFoundException("Service not found: " + id));
        if (request.name() != null) {
            String name = name(request.name());
            if (services.findByName(name).filter(other -> !other.getId().equals(id)).isPresent()) {
                throw new ConflictException("SERVICE_NAME_TAKEN", "There is a service with this name already");
            }
            service.setName(name);
        }
        if (request.icon() != null) {
            service.setIcon(icon(request.icon()));
        }
        applyOptional(service, request);
        return AdminServiceItem.of(service);
    }

    @Transactional(readOnly = true)
    public List<AdminUnitItem> units() {
        Map<PricingUnit, String> icons = catalog.unitIcons();
        return Arrays.stream(PricingUnit.values())
                .map(unit -> new AdminUnitItem(unit.name(), unit.phrase(), icons.get(unit))).toList();
    }

    /** @throws NotFoundException if there is no such unit */
    @Transactional
    public AdminUnitItem setUnitIcon(String key, UnitIconRequest request) {
        PricingUnit unit = PricingUnit.fromKey(key).orElseThrow(() -> new NotFoundException("Unit not found: " + key));
        String icon = icon(request.icon());
        UnitIcon row = unitIcons.findById(unit.name()).orElseGet(() -> new UnitIcon(unit, icon));
        row.setIcon(icon);
        unitIcons.save(row);
        return new AdminUnitItem(unit.name(), unit.phrase(), icon);
    }

    private static void applyOptional(ServiceType service, ServiceRequest request) {
        if (request.sortOrder() != null) {
            if (request.sortOrder() < 0 || request.sortOrder() > MAX_SORT_ORDER) {
                throw new BadRequestException("The order must be between 0 and " + MAX_SORT_ORDER);
            }
            service.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }
        if (request.asksName() != null) {
            service.setAsksName(request.asksName());
        }
    }

    private static String name(String raw) {
        String name = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw new BadRequestException("The name is required and can be at most " + MAX_NAME + " characters");
        }
        return name;
    }

    private static String icon(String raw) {
        String icon = raw == null ? "" : raw.strip();
        if (icon.isEmpty() || icon.length() > MAX_ICON) {
            throw new BadRequestException("The icon is required and can be at most " + MAX_ICON + " characters");
        }
        return icon;
    }
}
