package com.shut.api.market;

import com.shut.api.venue.LabelResponse;

import java.util.List;

public record MarketCatalogResponse(List<CategoryItem> categories, List<LabelResponse> conditions,
                                    int maxActiveListings, int maxPhotos) {
}
