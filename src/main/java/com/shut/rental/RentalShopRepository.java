package com.shut.rental;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RentalShopRepository extends JpaRepository<RentalShop, Long> {

    Optional<RentalShop> findByOwnerId(Long ownerId);

    /** Shops customers can see: active, reachable, and with at least one item available. */
    @Query("""
            select s from RentalShop s
            where s.active = true and s.phone is not null and s.city is not null and s.address is not null
              and exists (select 1 from RentalItem i where i.shopId = s.id and i.available = true)
            order by s.id desc
            """)
    Page<RentalShop> listed(Pageable pageable);
}
