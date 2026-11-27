package com.shut.rental;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RentalItemPhotoRepository extends JpaRepository<RentalItemPhoto, Long> {

    List<RentalItemPhoto> findByItemIdOrderByIdAsc(Long itemId);

    List<RentalItemPhoto> findByItemIdIn(Collection<Long> itemIds);

    long countByItemId(Long itemId);

    boolean existsByItemIdAndFileUniqueId(Long itemId, String fileUniqueId);
}
