package com.shut.market;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    /** Match-everything value for {@code category}. */
    long ANY_CATEGORY = -1L;

    /**
     * The listings others can see, newest first.
     *
     * @param pattern lower case and wrapped in {@code %}; matches the title, brand or model
     * @param city    lower case and wrapped in {@code %}
     * @param category an equipment type id, or {@link #ANY_CATEGORY}
     */
    @Query("""
            select l from Listing l
            where l.status = com.shut.market.ListingStatus.ACTIVE and l.hidden = false
              and (:category = -1 or l.categoryId = :category)
              and lower(coalesce(l.city, '')) like :city
              and (lower(l.title) like :pattern or lower(coalesce(l.brand, '')) like :pattern
                   or lower(coalesce(l.model, '')) like :pattern)
            order by l.id desc
            """)
    Page<Listing> searchPublic(@Param("pattern") String pattern, @Param("city") String city,
                               @Param("category") long category, Pageable pageable);

    Optional<Listing> findByIdAndSellerId(Long id, Long sellerId);

    List<Listing> findBySellerIdOrderByIdDesc(Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, ListingStatus status);
}
