package com.shut.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByOwnerIdOrderByIdAsc(Long ownerId);

    long countByOwnerId(Long ownerId);

    Optional<Venue> findByIdAndOwnerId(Long id, Long ownerId);

    /** The venues other members can see, newest first. */
    @Query("""
            select v from Venue v
            where v.active = true and v.city is not null
              and exists (select 1 from VenuePrice p where p.venueId = v.id)
            order by v.id desc
            """)
    Page<Venue> listed(Pageable pageable);
}
