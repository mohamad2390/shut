package com.shut.api.admin;

import com.shut.pricing.ServiceType;

/** A service of the price menu, also when it is switched off. */
public record AdminServiceItem(Long id, String name, String icon, int sortOrder, boolean active, boolean asksName) {

    static AdminServiceItem of(ServiceType service) {
        return new AdminServiceItem(service.getId(), service.getName(), service.getIcon(), service.getSortOrder(),
                service.isActive(), service.asksName());
    }
}
