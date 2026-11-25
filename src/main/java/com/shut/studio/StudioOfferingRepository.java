package com.shut.studio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudioOfferingRepository extends JpaRepository<StudioOffering, Long> {

    List<StudioOffering> findByStudioIdOrderByIdAsc(Long studioId);

    long countByStudioId(Long studioId);

    boolean existsByStudioId(Long studioId);

    Optional<StudioOffering> findByIdAndStudioId(Long id, Long studioId);
}
