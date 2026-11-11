package com.shut.profile.repository;

import com.shut.profile.entity.ProfessionalProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long> {
    Optional<ProfessionalProfile> findByUser_Id(Long userId);

    /**
     * Active members with a display name, other than {@code excludeUserId}, whose name, headline, city,
     * skills or service cities contain the pattern. {@code pattern} is a lower-case SQL LIKE pattern
     * that uses backslash as its escape character.
     */
    @Query(value = """
            select distinct p from ProfessionalProfile p
            left join p.skills skill
            left join p.serviceCities serviceCity
            where p.user.active = true
              and p.user.id <> :excludeUserId
              and p.displayName is not null
              and (lower(p.displayName) like :pattern escape '\\'
                or lower(coalesce(p.headline, '')) like :pattern escape '\\'
                or lower(coalesce(p.baseCity, '')) like :pattern escape '\\'
                or lower(coalesce(skill, '')) like :pattern escape '\\'
                or lower(coalesce(serviceCity, '')) like :pattern escape '\\')
            order by p.displayName, p.id
            """,
            countQuery = """
            select count(distinct p) from ProfessionalProfile p
            left join p.skills skill
            left join p.serviceCities serviceCity
            where p.user.active = true
              and p.user.id <> :excludeUserId
              and p.displayName is not null
              and (lower(p.displayName) like :pattern escape '\\'
                or lower(coalesce(p.headline, '')) like :pattern escape '\\'
                or lower(coalesce(p.baseCity, '')) like :pattern escape '\\'
                or lower(coalesce(skill, '')) like :pattern escape '\\'
                or lower(coalesce(serviceCity, '')) like :pattern escape '\\')
            """)
    Page<ProfessionalProfile> search(@Param("pattern") String pattern,
                                     @Param("excludeUserId") Long excludeUserId,
                                     Pageable pageable);
}
