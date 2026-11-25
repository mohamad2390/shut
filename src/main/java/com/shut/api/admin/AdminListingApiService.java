package com.shut.api.admin;

import com.shut.api.BadRequestException;
import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.api.venue.LabelResponse;
import com.shut.market.Listing;
import com.shut.market.ListingNotFoundException;
import com.shut.market.MarketService;
import com.shut.market.OfferService;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Every listing of the market, and hiding one (or showing it again) whoever sells it. */
@Service
public class AdminListingApiService {

    private final MarketService market;
    private final OfferService offers;
    private final UserService users;

    public AdminListingApiService(MarketService market, OfferService offers, UserService users) {
        this.market = market;
        this.offers = offers;
        this.users = users;
    }

    public PageResponse<AdminListingItem> list(int page, int size) {
        PageRequest paging = PageRequest.of(Paging.page(page), Paging.size(size), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(market.all(paging), this::item);
    }

    /** @throws ListingNotFoundException if there is no such listing */
    public AdminListingItem setHidden(Long id, Boolean hidden) {
        if (hidden == null) {
            throw new BadRequestException("`hidden` must be true or false");
        }
        return item(market.setHidden(id, hidden).orElseThrow(() -> new ListingNotFoundException(id)));
    }

    private AdminListingItem item(Listing listing) {
        User seller = users.findById(listing.getSellerId());
        String name = ((seller.getFirstName() == null ? "" : seller.getFirstName()) + " "
                + (seller.getLastName() == null ? "" : seller.getLastName())).trim();
        return new AdminListingItem(listing.getId(), listing.getTitle(), listing.getPrice(),
                new LabelResponse(listing.getStatus().name(), listing.getStatus().label()), listing.isHidden(),
                seller.getId(), name.isEmpty() ? seller.getPhoneNumber() : name, market.photoCount(listing.getId()),
                offers.pendingCount(listing.getId()));
    }
}
