package com.shut.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByProviderIdAndReviewerId(Long providerId, Long reviewerId);

    Page<Review> findByProviderId(Long providerId, Pageable pageable);

    @Query("select avg(r.rating) as average, count(r) as total from Review r where r.providerId = :providerId")
    Aggregate aggregate(@Param("providerId") Long providerId);

    /** The average is null when there are no reviews. */
    interface Aggregate {
        Double getAverage();

        long getTotal();
    }
}
