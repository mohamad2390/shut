package com.shut.api.market;

import com.shut.api.BadRequestException;
import com.shut.api.ConflictException;
import com.shut.api.NotFoundException;
import com.shut.market.Listing;
import com.shut.market.ListingCondition;
import com.shut.market.ListingNotFoundException;
import com.shut.market.ListingStatus;
import com.shut.market.MarketEvents;
import com.shut.market.MarketService;
import com.shut.market.Offer;
import com.shut.market.OfferService;
import com.shut.pricing.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A seller's own listings: create, change, mark sold or closed, and answer offers. Photos can only be added in the
 * Telegram bot, and so can deleting a listing; "closed" takes it off the market on the web.
 */
@Service
public class MyMarketApiService {

    private static final Logger log = LoggerFactory.getLogger(MyMarketApiService.class);

    private final MarketService market;
    private final OfferService offers;
    private final ListingViews views;
    private final ObjectProvider<MarketEvents> events;

    public MyMarketApiService(MarketService market, OfferService offers, ListingViews views,
                              ObjectProvider<MarketEvents> events) {
        this.market = market;
        this.offers = offers;
        this.views = views;
        this.events = events;
    }

    public List<MyListing> list(Long sellerId) {
        return market.ownedBy(sellerId).stream().map(views::mine).toList();
    }

    public MyListing details(Long sellerId, Long id) {
        return views.mine(owned(sellerId, id));
    }

    /**
     * @throws BadRequestException if a required field is missing or a value is not valid
     * @throws ConflictException   if the seller already has the maximum number of active listings
     */
    public MyListing create(Long sellerId, ListingRequest request) {
        String title = required(request.title(), 120, "The title");
        ListingCondition condition = ListingCondition.fromKey(request.condition())
                .orElseThrow(() -> new BadRequestException("The condition is required"));
        long price = price(request.price());
        Fields fields = Fields.of(request, views);
        Listing listing = market.create(sellerId, title, condition, price, fields::apply)
                .orElseThrow(() -> new ConflictException("LISTING_LIMIT_REACHED",
                        "You can have at most " + MarketService.MAX_ACTIVE_LISTINGS_PER_SELLER + " active listings"));
        return views.mine(listing);
    }

    /** Saves the fields that are sent. Nothing changes if a value is not valid. */
    public MyListing update(Long sellerId, Long id, ListingRequest request) {
        owned(sellerId, id);
        String title = request.title() == null ? null : required(request.title(), 120, "The title");
        ListingCondition condition = request.condition() == null ? null : ListingCondition.fromKey(request.condition())
                .orElseThrow(() -> new BadRequestException("Unknown condition"));
        Long price = request.price() == null ? null : price(request.price());
        Fields fields = Fields.of(request, views);
        Listing listing = market.update(id, sellerId, found -> {
            if (title != null) found.setTitle(title);
            if (condition != null) found.setCondition(condition);
            if (price != null) found.setPrice(price);
            fields.apply(found);
        }).orElseThrow(() -> new ListingNotFoundException(id));
        return views.mine(listing);
    }

    /** @throws ConflictException if reopening would go over the limit of active listings */
    public MyListing setStatus(Long sellerId, Long id, StatusRequest request) {
        ListingStatus status = ListingStatus.fromKey(request.status())
                .orElseThrow(() -> new BadRequestException("Unknown status"));
        owned(sellerId, id);
        Listing listing = market.setStatus(id, sellerId, status)
                .orElseThrow(() -> new ConflictException("LISTING_LIMIT_REACHED",
                        "You can have at most " + MarketService.MAX_ACTIVE_LISTINGS_PER_SELLER + " active listings"));
        return views.mine(listing);
    }

    public List<SellerOfferResponse> offers(Long sellerId, Long id) {
        owned(sellerId, id);
        return offers.pending(id, sellerId).stream().map(offer -> new SellerOfferResponse(offer.getId(),
                offer.getAmount(), offer.getMessage(), offer.getCreatedAt(), views.contact(offer.getBuyerId()))).toList();
    }

    /**
     * @throws NotFoundException if the offer is not on this listing
     * @throws ConflictException if it was answered or taken back already
     */
    public void answer(Long sellerId, Long id, Long offerId, Boolean accept) {
        if (accept == null) {
            throw new BadRequestException("`accept` must be true or false");
        }
        Listing listing = owned(sellerId, id);
        offers.find(offerId).filter(offer -> offer.getListingId().equals(id))
                .orElseThrow(() -> new NotFoundException("Offer not found: " + offerId));
        Offer answered = offers.respond(offerId, sellerId, accept)
                .orElseThrow(() -> new ConflictException("OFFER_ALREADY_ANSWERED", "This offer is not waiting any more"));
        MarketEvents telegram = events.getIfAvailable();
        if (telegram != null) {
            try {
                telegram.offerAnswered(listing, answered);
            } catch (RuntimeException e) {
                log.warn("Could not tell the buyer of offer {} about the answer: {}", offerId, e.getMessage());
            }
        }
    }

    private Listing owned(Long sellerId, Long id) {
        return market.findOwned(id, sellerId).orElseThrow(() -> new ListingNotFoundException(id));
    }

    private static long price(Long amount) {
        if (amount == null || amount <= 0 || amount > PricingService.MAX_AMOUNT) {
            throw new BadRequestException("The price must be between 1 and " + PricingService.MAX_AMOUNT + " tomans");
        }
        return amount;
    }

    private static String clean(String raw) {
        return raw.strip().replaceAll("\\s+", " ");
    }

    private static String required(String raw, int max, String what) {
        String value = raw == null ? "" : clean(raw);
        if (value.isEmpty() || value.length() > max) {
            throw new BadRequestException(what + " is required and can be at most " + max + " characters");
        }
        return value;
    }

    /** Null: not sent. Empty: clear. */
    private static String optional(String raw, int max, String what) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = clean(raw);
        if (value.length() > max) {
            throw new BadRequestException(what + " can be at most " + max + " characters");
        }
        return value;
    }

    /** The optional fields of a request, checked, ready to put on a listing. */
    private record Fields(ListingRequest request, String brand, String model, String city, String description,
                          Long categoryId) {

        static Fields of(ListingRequest request, ListingViews views) {
            Long category = null;
            if (request.categoryId() != null && request.categoryId() != 0) {
                category = views.category(request.categoryId())
                        .orElseThrow(() -> new BadRequestException("Unknown category")).id();
            }
            return new Fields(request, optional(request.brand(), 60, "The brand"),
                    optional(request.model(), 80, "The model"), optional(request.city(), 100, "The city"),
                    request.description() == null || request.description().isBlank() ? null
                            : limited(request.description().strip(), 2000, "The description"),
                    category);
        }

        private static String limited(String value, int max, String what) {
            if (value.length() > max) {
                throw new BadRequestException(what + " can be at most " + max + " characters");
            }
            return value;
        }

        void apply(Listing listing) {
            if (request.brand() != null) listing.setBrand(brand);
            if (request.model() != null) listing.setModel(model);
            if (request.city() != null) listing.setCity(city);
            if (request.description() != null) listing.setDescription(description);
            if (request.categoryId() != null) listing.setCategoryId(categoryId);
            if (request.negotiable() != null) listing.setNegotiable(request.negotiable());
        }
    }
}
