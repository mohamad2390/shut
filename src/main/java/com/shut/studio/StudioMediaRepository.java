package com.shut.studio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudioMediaRepository extends JpaRepository<StudioMedia, Long> {

    List<StudioMedia> findByStudioIdOrderByIdAsc(Long studioId);

    long countByStudioId(Long studioId);

    boolean existsByStudioIdAndFileUniqueId(Long studioId, String fileUniqueId);
}
