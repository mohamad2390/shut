package com.shut.profile.repository;

import com.shut.profile.entity.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, Long> {
    Optional<ProfessionalProfile> findByUser_Id(Long userId);
}
