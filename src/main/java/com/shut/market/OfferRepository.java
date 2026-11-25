package com.shut.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByListingIdAndStatusOrderByIdAsc(Long listingId, OfferStatus status);

    long countByListingIdAndStatus(Long listingId, OfferStatus status);

    boolean existsByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, OfferStatus status);

    List<Offer> findTop20ByBuyerIdOrderByIdDesc(Long buyerId);
}
