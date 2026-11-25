package com.shut.api.venue;

import com.shut.common.web.ApiPaths;
import com.shut.venue.VenuePriceUnit;
import com.shut.venue.VenueService;
import com.shut.venue.VenueType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/** Public: the kinds of venue and the price units, so the web forms offer the same choices as the bot. */
@RestController
public class VenueCatalogController {

    @GetMapping(ApiPaths.Venues.BASE + ApiPaths.Venues.CATALOG)
    public VenueCatalogResponse catalog() {
        return new VenueCatalogResponse(
                Arrays.stream(VenueType.values()).map(type -> new VenueCatalogResponse.TypeItem(
                        type.name(), type.label(), type.icon())).toList(),
                Arrays.stream(VenuePriceUnit.values()).map(unit -> new LabelResponse(unit.name(), unit.label())).toList(),
                VenueService.MAX_VENUES_PER_OWNER, VenueService.MAX_PRICES, VenueService.MAX_PHOTOS);
    }
}
