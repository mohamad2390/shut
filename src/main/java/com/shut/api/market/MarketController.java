package com.shut.api.market;

import com.shut.api.PageResponse;
import com.shut.api.me.MemberAccess;
import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The gear market everyone can browse; making an offer needs a sign-in. */
@RestController
@RequestMapping(ApiPaths.Market.BASE)
public class MarketController {

    private final MemberAccess access;
    private final MarketApiService market;

    public MarketController(MemberAccess access, MarketApiService market) {
        this.access = access;
        this.market = market;
    }

    @GetMapping(ApiPaths.Market.CATALOG)
    public MarketCatalogResponse catalog() {
        return market.catalog();
    }

    @GetMapping(ApiPaths.Market.LISTINGS)
    public PageResponse<ListingListItem> search(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String city,
                                                @RequestParam(required = false) Long category,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "0") int size) {
        return market.search(q, city, category, page, size);
    }

    @GetMapping(ApiPaths.Market.LISTING)
    public ListingDetails details(@PathVariable Long id) {
        return market.details(id);
    }

    @PostMapping(ApiPaths.Market.LISTING_OFFERS)
    @ResponseStatus(HttpStatus.CREATED)
    public MyOfferResponse offer(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long id, @RequestBody OfferRequest request) {
        return market.makeOffer(access.userId(authorization), id, request);
    }
}
