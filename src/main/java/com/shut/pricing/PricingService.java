package com.shut.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    /** The item if it belongs to the user. */
    @Transactional(readOnly = true)
    public Optional<PriceItem> findOwned(Long itemId, Long userId) {
        return repository.findByIdAndUserId(itemId, userId);
    }

    /** Changes the price of the user's item. Returns the updated item, or empty if it is not theirs. */
    @Transactional
    public Optional<PriceItem> updateAmount(Long itemId, Long userId, long amount) {
        return repository.findByIdAndUserId(itemId, userId).map(item -> {
            item.changeAmount(amount);
            return item;
        });
    }

    /** Changes what the price is for (per hour, per photo...). Returns the updated item, or empty if it is not theirs. */
    @Transactional
    public Optional<PriceItem> updateUnit(Long itemId, Long userId, PricingUnit unit) {
        return repository.findByIdAndUserId(itemId, userId).map(item -> {
            item.changeUnit(unit);
            return item;
        });
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
