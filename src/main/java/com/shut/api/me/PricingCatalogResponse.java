package com.shut.api.me;

import java.util.List;

/** The choices when adding a price, and the limits. */
public record PricingCatalogResponse(List<ServiceTypeResponse> services, List<UnitResponse> units, int maxItems,
                                     long maxAmount) {
}
