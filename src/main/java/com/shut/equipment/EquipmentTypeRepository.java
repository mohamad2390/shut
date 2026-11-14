package com.shut.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    Optional<EquipmentType> findByNormalizedName(String normalizedName);

    List<EquipmentType> findAllByOrderBySortOrderAscIdAsc();
}
