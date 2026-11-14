package com.shut.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEquipmentRepository extends JpaRepository<UserEquipment, Long> {

    List<UserEquipment> findByUserIdOrderByIdAsc(long userId);

    boolean existsByUserIdAndModel_Id(long userId, long modelId);

    long countByUserId(long userId);

    Optional<UserEquipment> findByIdAndUserId(long id, long userId);
}
