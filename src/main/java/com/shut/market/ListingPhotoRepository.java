package com.shut.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, Long> {

    List<ListingPhoto> findByListingIdOrderByIdAsc(Long listingId);

    long countByListingId(Long listingId);

    boolean existsByListingIdAndFileUniqueId(Long listingId, String fileUniqueId);
}
