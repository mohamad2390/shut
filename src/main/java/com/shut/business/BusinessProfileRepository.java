package com.shut.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    Optional<BusinessProfile> findByUserIdAndKind(Long userId, BusinessKind kind);

    List<BusinessProfile> findByUserId(Long userId);
}
