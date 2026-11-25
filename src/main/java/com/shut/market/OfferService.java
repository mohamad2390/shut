package com.shut.market;

import com.shut.pricing.PricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Offers on listings. Accepting an offer does not close the listing: the seller marks it sold when it is. */
@Service
public class OfferService {

    private final OfferRepository offers;
    private final ListingRepository listings;

    public OfferService(OfferRepository offers, ListingRepository listings) {
        this.offers = offers;
        this.listings = listings;
    }

    /**
     * @param amount in tomans, or null to take it at the asking price
     * @throws MarketException if the listing is not shown, is the buyer's own, the amount is not valid, or the buyer
     *                         has an offer on it that waits for an answer
     */
    @Transactional
    public Offer make(Long listingId, Long buyerId, Long amount, String message) {
        Listing listing = listings.findById(listingId).filter(Listing::isPublic)
                .orElseThrow(() -> new MarketException(MarketException.Reason.LISTING_NOT_AVAILABLE));
        if (listing.getSellerId().equals(buyerId)) {
            throw new MarketException(MarketException.Reason.OWN_LISTING);
        }
        if (amount != null && (amount <= 0 || amount > PricingService.MAX_AMOUNT)) {
            throw new MarketException(MarketException.Reason.INVALID_AMOUNT);
        }
        if (offers.existsByListingIdAndBuyerIdAndStatus(listingId, buyerId, OfferStatus.PENDING)) {
            throw new MarketException(MarketException.Reason.ALREADY_OFFERED);
        }
        return offers.save(new Offer(listingId, buyerId, amount, message));
    }

    @Transactional(readOnly = true)
    public Optional<Offer> find(Long offerId) {
        return offers.findById(offerId);
    }

    /** The offers of the seller's listing that wait for an answer, oldest first. Empty for someone else's listing. */
    @Transactional(readOnly = true)
    public List<Offer> pending(Long listingId, Long sellerId) {
        return listings.findByIdAndSellerId(listingId, sellerId)
                .map(listing -> offers.findByListingIdAndStatusOrderByIdAsc(listingId, OfferStatus.PENDING))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public long pendingCount(Long listingId) {
        return offers.countByListingIdAndStatus(listingId, OfferStatus.PENDING);
    }

    /**
     * The seller's answer to an offer that still waits. Empty if there is no such offer, it is not on one of the
     * seller's listings, or it was answered already.
     */
    @Transactional
    public Optional<Offer> respond(Long offerId, Long sellerId, boolean accept) {
        return offers.findById(offerId)
                .filter(offer -> offer.getStatus() == OfferStatus.PENDING)
                .filter(offer -> listings.findByIdAndSellerId(offer.getListingId(), sellerId).isPresent())
                .map(offer -> {
                    offer.answer(accept ? OfferStatus.ACCEPTED : OfferStatus.DECLINED);
                    return offer;
                });
    }

    /** The buyer takes their offer back. Empty if it is not theirs or is not waiting any more. */
    @Transactional
    public Optional<Offer> withdraw(Long offerId, Long buyerId) {
        return offers.findById(offerId)
                .filter(offer -> offer.getBuyerId().equals(buyerId))
                .filter(offer -> offer.getStatus() == OfferStatus.PENDING)
                .map(offer -> {
                    offer.answer(OfferStatus.WITHDRAWN);
                    return offer;
                });
    }

    /** The buyer's 20 latest offers, newest first. */
    @Transactional(readOnly = true)
    public List<Offer> latestOf(Long buyerId) {
        return offers.findTop20ByBuyerIdOrderByIdDesc(buyerId);
    }
}
