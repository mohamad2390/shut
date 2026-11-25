package com.shut.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneOrderByIdDesc(String phone);

    Optional<OtpCode> findFirstByPhoneAndUsedFalseOrderByIdDesc(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, OffsetDateTime since);
}
