package com.shut.project;

import com.shut.showcase.ShowcaseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    long countByOwnerIdAndStatus(Long ownerId, ProjectStatus status);

    /** Projects that still take offers, other than the viewer's own. Sort with the pageable. */
    @Query("""
            select p from Project p
            where p.status = com.shut.project.ProjectStatus.OPEN and p.closesAt > :now and p.ownerId <> :viewerId
            """)
    Page<Project> findTakingBids(@Param("now") OffsetDateTime now, @Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
            select p from Project p
            where p.status = com.shut.project.ProjectStatus.OPEN and p.closesAt > :now and p.ownerId <> :viewerId
              and p.category = :category
            """)
    Page<Project> findTakingBidsInCategory(@Param("now") OffsetDateTime now, @Param("viewerId") Long viewerId,
                                           @Param("category") ShowcaseCategory category, Pageable pageable);

    /** Open projects whose deadline has passed and whose owner has not been told yet. */
    List<Project> findByStatusAndClosingNotifiedFalseAndClosesAtLessThanEqual(ProjectStatus status, OffsetDateTime now);
}
