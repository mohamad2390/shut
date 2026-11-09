package com.shut.showcase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShowcaseItemRepository extends JpaRepository<ShowcaseItem, Long> {

    long countByUserIdAndCategory(Long userId, ShowcaseCategory category);

    boolean existsByUserIdAndFileUniqueId(Long userId, String fileUniqueId);

    Page<ShowcaseItem> findByUserIdAndCategory(Long userId, ShowcaseCategory category, Pageable pageable);

    Optional<ShowcaseItem> findByIdAndUserId(Long id, Long userId);

    @Query("select i.category as category, count(i) as total from ShowcaseItem i "
            + "where i.userId = :userId group by i.category")
    List<CategoryCount> countByCategory(@Param("userId") Long userId);
}
