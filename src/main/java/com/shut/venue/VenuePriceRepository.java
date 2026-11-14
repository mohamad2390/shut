package com.shut.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VenuePriceRepository extends JpaRepository<VenuePrice, Long> {

    List<VenuePrice> findByVenueIdOrderByIdAsc(Long venueId);

    List<VenuePrice> findByVenueIdInOrderByIdAsc(Collection<Long> venueIds);

    long countByVenueId(Long venueId);

    boolean existsByVenueId(Long venueId);
}
