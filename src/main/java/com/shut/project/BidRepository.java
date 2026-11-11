package com.shut.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    Optional<Bid> findByProjectIdAndBidderId(Long projectId, Long bidderId);

    Page<Bid> findByProjectId(Long projectId, Pageable pageable);

    List<Bid> findAllByProjectId(Long projectId);

    long countByProjectId(Long projectId);

    Page<Bid> findByBidderId(Long bidderId, Pageable pageable);
}
