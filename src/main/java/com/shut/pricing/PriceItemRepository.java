package com.shut.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceItemRepository extends JpaRepository<PriceItem, Long> {

    long countByUserId(Long userId);

    List<PriceItem> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    Optional<PriceItem> findByIdAndUserId(Long id, Long userId);
}
