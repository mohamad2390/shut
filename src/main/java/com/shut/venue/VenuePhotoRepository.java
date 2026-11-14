package com.shut.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenuePhotoRepository extends JpaRepository<VenuePhoto, Long> {

    List<VenuePhoto> findByVenueIdOrderByIdAsc(Long venueId);

    long countByVenueId(Long venueId);

    boolean existsByVenueIdAndFileUniqueId(Long venueId, String fileUniqueId);
}
