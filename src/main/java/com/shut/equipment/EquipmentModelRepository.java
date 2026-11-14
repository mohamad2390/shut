package com.shut.equipment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    Optional<EquipmentModel> findByType_IdAndBrand_IdAndNormalizedName(long typeId, long brandId, String normalizedName);

    /** Models of one brand and type: the ones most members own come first, then the ones we have checked. */
    @Query("""
            select m from EquipmentModel m
            left join UserEquipment u on u.model = m
            where m.type.id = :typeId and m.brand.id = :brandId
            group by m
            order by count(u) desc, m.verified desc, m.name
            """)
    List<EquipmentModel> modelsFor(@Param("typeId") long typeId, @Param("brandId") long brandId, Pageable pageable);
}
