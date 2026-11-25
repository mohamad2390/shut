package com.shut.api.me;

import com.shut.common.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The signed-in specialist's own price menu. */
@RestController
@RequestMapping(ApiPaths.Me.BASE)
public class MyPricesController {

    private final MemberAccess access;
    private final PricingApiService prices;

    public MyPricesController(MemberAccess access, PricingApiService prices) {
        this.access = access;
        this.prices = prices;
    }

    @GetMapping(ApiPaths.Me.PRICES)
    public List<PriceItemResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return prices.list(access.providerId(authorization));
    }

    @PostMapping(ApiPaths.Me.PRICES)
    @ResponseStatus(HttpStatus.CREATED)
    public PriceItemResponse add(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody CreatePriceRequest request) {
        return prices.add(access.providerId(authorization), request);
    }

    @PatchMapping(ApiPaths.Me.PRICE)
    public PriceItemResponse update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long id, @RequestBody UpdatePriceRequest request) {
        return prices.update(access.providerId(authorization), id, request);
    }

    @DeleteMapping(ApiPaths.Me.PRICE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable Long id) {
        prices.delete(access.providerId(authorization), id);
    }
}
