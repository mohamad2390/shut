package com.shut.api.market;

import com.shut.api.BadRequestException;
import com.shut.api.NotFoundException;
import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.api.venue.LabelResponse;
import com.shut.market.Listing;
import com.shut.market.ListingCondition;
import com.shut.market.ListingNotFoundException;
import com.shut.market.MarketEvents;
import com.shut.market.MarketService;
import com.shut.market.Offer;
import com.shut.market.OfferService;
import com.shut.market.OfferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/** What everyone sees of the market, and what a buyer can do: make an offer, see theirs, take one back. */
@Service
public class MarketApiService {

    static final int MAX_MESSAGE_LENGTH = 500;
    private static final Logger log = LoggerFactory.getLogger(MarketApiService.class);

    private final MarketService market;
    private final OfferService offers;
    private final ListingViews views;
    private final ObjectProvider<MarketEvents> events;

    public MarketApiService(MarketService market, OfferService offers, ListingViews views,
                            ObjectProvider<MarketEvents> events) {
        this.market = market;
        this.offers = offers;
        this.views = views;
        this.events = events;
    }

    public MarketCatalogResponse catalog() {
        return new MarketCatalogResponse(views.categories(),
                Arrays.stream(ListingCondition.values()).map(ListingViews::condition).toList(),
                MarketService.MAX_ACTIVE_LISTINGS_PER_SELLER, MarketService.MAX_PHOTOS);
    }

    public PageResponse<ListingListItem> search(String q, String city, Long category, int page, int size) {
        var found = market.search(q, city, category, Paging.page(page), Paging.size(size));
        List<ListingListItem> items = views.items(found.getContent());
        return new PageResponse<>(items, found.getNumber(), found.getSize(), found.getTotalElements(),
                found.getTotalPages());
    }

    /** @throws ListingNotFoundException if there is no such listing or others cannot see it */
    public ListingDetails details(Long id) {
        return views.details(market.findPublic(id).orElseThrow(() -> new ListingNotFoundException(id)));
    }

    /**
     * @throws com.shut.market.MarketException if the offer cannot be made
     * @throws BadRequestException              if the message is too long
     */
    public MyOfferResponse makeOffer(Long buyerId, Long listingId, OfferRequest request) {
        String message = request.message() == null ? "" : request.message().strip();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("The message can be at most " + MAX_MESSAGE_LENGTH + " characters");
        }
        Offer offer = offers.make(listingId, buyerId, request.amount(), message.isEmpty() ? null : message);
        Listing listing = market.find(listingId).orElseThrow(() -> new ListingNotFoundException(listingId));
        MarketEvents telegram = events.getIfAvailable();
        if (telegram != null) {
            try {
                telegram.offerArrived(listing, offer);
            } catch (RuntimeException e) {
                // The offer is saved; the seller will still see it in their list.
                log.warn("Could not tell the seller of listing {} about offer {}: {}", listingId, offer.getId(),
                        e.getMessage());
            }
        }
        return mine(offer, listing);
    }

    /** The member's 20 latest offers, newest first. */
    public List<MyOfferResponse> myOffers(Long buyerId) {
        return offers.latestOf(buyerId).stream()
                .map(offer -> mine(offer, market.find(offer.getListingId()).orElseThrow())).toList();
    }

    /** @throws NotFoundException if it is not the member's offer or is not waiting any more */
    public void withdraw(Long buyerId, Long offerId) {
        offers.withdraw(offerId, buyerId).orElseThrow(() -> new NotFoundException("Offer not found: " + offerId));
    }

    private MyOfferResponse mine(Offer offer, Listing listing) {
        ContactResponse seller = offer.getStatus() == OfferStatus.ACCEPTED ? views.contact(listing.getSellerId()) : null;
        return new MyOfferResponse(offer.getId(), listing.getId(), listing.getTitle(), offer.getAmount(),
                offer.getMessage(), new LabelResponse(offer.getStatus().name(), offer.getStatus().label()),
                offer.getCreatedAt(), seller);
    }
}
