package com.shut.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentBrandAliasRepository extends JpaRepository<EquipmentBrandAlias, Long> {

    Optional<EquipmentBrandAlias> findByNormalizedAlias(String normalizedAlias);
}
