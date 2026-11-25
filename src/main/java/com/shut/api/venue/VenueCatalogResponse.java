package com.shut.api.venue;

import java.util.List;

/** The choices when describing a venue and pricing it. */
public record VenueCatalogResponse(List<TypeItem> types, List<LabelResponse> priceUnits, int maxVenuesPerOwner,
                                   int maxPrices, int maxPhotos) {

    /** A kind of venue with its icon. */
    public record TypeItem(String key, String label, String icon) {
    }
}
