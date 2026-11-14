package com.shut.equipment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentBrandRepository extends JpaRepository<EquipmentBrand, Long> {

    Optional<EquipmentBrand> findByNormalizedName(String normalizedName);

    /** Brands that make the most models of the type come first, then the ones we have checked, then by name. */
    @Query("""
            select b from EquipmentBrand b
            left join EquipmentModel m on m.brand = b and m.type.id = :typeId
            group by b
            order by count(m) desc, b.verified desc, b.name
            """)
    List<EquipmentBrand> brandsFor(@Param("typeId") long typeId, Pageable pageable);
}
