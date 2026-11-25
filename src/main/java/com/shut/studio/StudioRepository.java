package com.shut.studio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudioRepository extends JpaRepository<Studio, Long> {

    Optional<Studio> findByOwnerId(Long ownerId);

    /** Studios others can see: active, with every contact detail and at least one service. */
    @Query("""
            select s from Studio s
            where s.active = true and s.managerName is not null and s.phone is not null
              and s.city is not null and s.address is not null
              and exists (select 1 from StudioOffering o where o.studioId = s.id)
            order by s.id desc
            """)
    Page<Studio> listed(Pageable pageable);
}
