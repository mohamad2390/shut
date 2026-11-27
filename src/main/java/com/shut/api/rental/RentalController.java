package com.shut.api.rental;

import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only view of the rental shops and what they rent out. */
@RestController
@RequestMapping(ApiPaths.Rental.BASE)
public class RentalController {

    private final RentalApiService rental;

    public RentalController(RentalApiService rental) {
        this.rental = rental;
    }

    @GetMapping(ApiPaths.Rental.SHOPS)
    public PageResponse<RentalShopListItem> shops(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "0") int size) {
        return rental.shops(Paging.page(page), Paging.size(size));
    }

    @GetMapping(ApiPaths.Rental.SHOP)
    public RentalShopDetailsResponse shop(@PathVariable Long id) {
        return rental.shop(id);
    }

    /** Items whose type, brand or model contains {@code q} (at least two characters), newest first. */
    @GetMapping(ApiPaths.Rental.ITEMS)
    public PageResponse<RentalSearchItem> search(@RequestParam String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "0") int size) {
        return rental.search(q, Paging.page(page), Paging.size(size));
    }

    @GetMapping(ApiPaths.Rental.ITEM)
    public RentalItemDetailsResponse item(@PathVariable Long id) {
        return rental.item(id);
    }
}
