package com.shut.studio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudioMemberRepository extends JpaRepository<StudioMember, Long> {

    Optional<StudioMember> findByStudioIdAndUserId(Long studioId, Long userId);

    List<StudioMember> findByStudioIdAndStatusOrderByIdAsc(Long studioId, MemberStatus status);

    long countByStudioId(Long studioId);

    long countByStudioIdAndStatus(Long studioId, MemberStatus status);

    boolean existsByStudioIdAndUserIdAndStatus(Long studioId, Long userId, MemberStatus status);
}
