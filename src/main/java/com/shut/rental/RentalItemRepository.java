package com.shut.rental;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    List<RentalItem> findByShopIdOrderByIdAsc(Long shopId);

    List<RentalItem> findByShopIdAndAvailableTrueOrderByIdAsc(Long shopId);

    long countByShopId(Long shopId);

    long countByShopIdAndAvailableTrue(Long shopId);

    Optional<RentalItem> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndModel_Id(Long shopId, Long modelId);

    /** Available items of shops customers can see whose type, brand or model contains the text. */
    @Query(value = """
            select i from RentalItem i
            where i.available = true
              and exists (select 1 from RentalShop s where s.id = i.shopId and s.active = true
                          and s.phone is not null and s.city is not null and s.address is not null)
              and (lower(i.model.name) like :pattern or lower(i.model.brand.name) like :pattern
                   or lower(i.model.type.name) like :pattern
                   or lower(concat(i.model.brand.name, ' ', i.model.name)) like :pattern)
            order by i.id desc
            """, countQuery = """
            select count(i) from RentalItem i
            where i.available = true
              and exists (select 1 from RentalShop s where s.id = i.shopId and s.active = true
                          and s.phone is not null and s.city is not null and s.address is not null)
              and (lower(i.model.name) like :pattern or lower(i.model.brand.name) like :pattern
                   or lower(i.model.type.name) like :pattern
                   or lower(concat(i.model.brand.name, ' ', i.model.name)) like :pattern)
            """)
    Page<RentalItem> search(@Param("pattern") String pattern, Pageable pageable);
}
