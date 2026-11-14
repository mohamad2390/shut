package com.shut.profile.repository;

import com.shut.profile.entity.ProfessionalProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long>, JpaSpecificationExecutor<ProfessionalProfile> {
    Optional<ProfessionalProfile> findByUser_Id(Long userId);

    /** Active members with a display name, best average rating first, then most ratings, then name. */
    @Query(value = """
            select p.user.id as userId, p.displayName as displayName, p.baseCity as city,
                   coalesce(avg(r.rating), 0.0) as average, count(r) as total
            from ProfessionalProfile p
            left join Review r on r.providerId = p.user.id
            where p.user.active = true and p.displayName is not null
            group by p.id, p.user.id, p.displayName, p.baseCity
            order by coalesce(avg(r.rating), 0.0) desc, count(r) desc, p.displayName, p.id
            """,
            countQuery = """
            select count(p) from ProfessionalProfile p
            where p.user.active = true and p.displayName is not null
            """)
    Page<RankingRow> ranking(Pageable pageable);

    /** Like {@link #ranking}, only for the given members. */
    @Query(value = """
            select p.user.id as userId, p.displayName as displayName, p.baseCity as city,
                   coalesce(avg(r.rating), 0.0) as average, count(r) as total
            from ProfessionalProfile p
            left join Review r on r.providerId = p.user.id
            where p.user.active = true and p.displayName is not null and p.user.id in :userIds
            group by p.id, p.user.id, p.displayName, p.baseCity
            order by coalesce(avg(r.rating), 0.0) desc, count(r) desc, p.displayName, p.id
            """,
            countQuery = """
            select count(p) from ProfessionalProfile p
            where p.user.active = true and p.displayName is not null and p.user.id in :userIds
            """)
    Page<RankingRow> rankingAmong(@Param("userIds") Collection<Long> userIds, Pageable pageable);

    /** The skills of the given members, one row per skill, in the order they were added. */
    @Query(value = """
            select p.user_id as "userId", s.skill as "skill"
            from professional_profile_skills s
            join professional_profiles p on p.id = s.profile_id
            where p.user_id in (:userIds)
            order by s.id
            """, nativeQuery = true)
    List<SkillRow> skillsOf(@Param("userIds") Collection<Long> userIds);

    interface SkillRow {
        Long getUserId();

        String getSkill();
    }

    interface RankingRow {
        Long getUserId();

        String getDisplayName();

        String getCity();

        Double getAverage();

        long getTotal();
    }
}
