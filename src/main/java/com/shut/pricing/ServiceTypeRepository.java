package com.shut.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

    List<ServiceType> findByActiveTrueOrderBySortOrderAscIdAsc();
}
