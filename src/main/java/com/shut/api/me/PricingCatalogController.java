package com.shut.api.me;

import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: the services and units a price can use, so the web forms can offer the same choices as the bot. */
@RestController
@RequestMapping(ApiPaths.Pricing.CATALOG)
public class PricingCatalogController {

    private final PricingApiService prices;

    public PricingCatalogController(PricingApiService prices) {
        this.prices = prices;
    }

    @GetMapping
    public PricingCatalogResponse catalog() {
        return prices.catalog();
    }
}
