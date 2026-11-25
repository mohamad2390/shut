package com.shut.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

    List<ServiceType> findByActiveTrueOrderBySortOrderAscIdAsc();

    /** Every service, also the ones that are switched off. */
    List<ServiceType> findAllByOrderBySortOrderAscIdAsc();

    Optional<ServiceType> findByName(String name);
}
