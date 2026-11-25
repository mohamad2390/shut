package com.shut.api.market;

import com.shut.api.me.MemberAccess;
import com.shut.api.me.venue.AnswerRequest;
import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The signed-in member's own listings and offers. Photos are only added in the Telegram bot. */
@RestController
@RequestMapping(ApiPaths.Me.BASE)
public class MyMarketController {

    private static final String AUTH = "Authorization";

    private final MemberAccess access;
    private final MyMarketApiService mine;
    private final MarketApiService market;

    public MyMarketController(MemberAccess access, MyMarketApiService mine, MarketApiService market) {
        this.access = access;
        this.mine = mine;
        this.market = market;
    }

    @GetMapping(ApiPaths.Me.LISTINGS)
    public List<MyListing> list(@RequestHeader(value = AUTH, required = false) String authorization) {
        return mine.list(access.userId(authorization));
    }

    @PostMapping(ApiPaths.Me.LISTINGS)
    @ResponseStatus(HttpStatus.CREATED)
    public MyListing create(@RequestHeader(value = AUTH, required = false) String authorization,
                            @RequestBody ListingRequest request) {
        return mine.create(access.userId(authorization), request);
    }

    @GetMapping(ApiPaths.Me.LISTING)
    public MyListing details(@RequestHeader(value = AUTH, required = false) String authorization,
                             @PathVariable Long id) {
        return mine.details(access.userId(authorization), id);
    }

    @PutMapping(ApiPaths.Me.LISTING)
    public MyListing update(@RequestHeader(value = AUTH, required = false) String authorization,
                            @PathVariable Long id, @RequestBody ListingRequest request) {
        return mine.update(access.userId(authorization), id, request);
    }

    @PutMapping(ApiPaths.Me.LISTING_STATUS)
    public MyListing setStatus(@RequestHeader(value = AUTH, required = false) String authorization,
                               @PathVariable Long id, @RequestBody StatusRequest request) {
        return mine.setStatus(access.userId(authorization), id, request);
    }

    @GetMapping(ApiPaths.Me.LISTING_OFFERS)
    public List<SellerOfferResponse> offers(@RequestHeader(value = AUTH, required = false) String authorization,
                                            @PathVariable Long id) {
        return mine.offers(access.userId(authorization), id);
    }

    @PutMapping(ApiPaths.Me.LISTING_OFFER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@RequestHeader(value = AUTH, required = false) String authorization, @PathVariable Long id,
                       @PathVariable Long offerId, @RequestBody AnswerRequest request) {
        mine.answer(access.userId(authorization), id, offerId, request.accept());
    }

    @GetMapping(ApiPaths.Me.OFFERS)
    public List<MyOfferResponse> myOffers(@RequestHeader(value = AUTH, required = false) String authorization) {
        return market.myOffers(access.userId(authorization));
    }

    @DeleteMapping(ApiPaths.Me.OFFER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@RequestHeader(value = AUTH, required = false) String authorization,
                         @PathVariable Long offerId) {
        market.withdraw(access.userId(authorization), offerId);
    }
}
