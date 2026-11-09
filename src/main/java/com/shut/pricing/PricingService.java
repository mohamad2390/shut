package com.shut.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Price menu rules: who owns which item and how many a menu may hold. */
@Service
public class PricingService {

    public static final int MAX_ITEMS = 20;

    private final PriceItemRepository repository;

    public PricingService(PriceItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PriceItem> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtAscIdAsc(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFull(Long userId) {
        return repository.countByUserId(userId) >= MAX_ITEMS;
    }

    /** Throws {@link PriceLimitReachedException} when the menu is full. */
    @Transactional
    public PriceItem add(Long userId, String title, PricingUnit unit, long amount) {
        if (isFull(userId)) {
            throw new PriceLimitReachedException();
        }
        return repository.save(new PriceItem(userId, title, unit, amount));
    }

    /** Deletes the item if it belongs to the user. Returns whether anything was deleted. */
    @Transactional
    public boolean deleteOwned(Long itemId, Long userId) {
        return repository.findByIdAndUserId(itemId, userId)
                .map(item -> {
                    repository.delete(item);
                    return true;
                })
                .orElse(false);
    }
}
