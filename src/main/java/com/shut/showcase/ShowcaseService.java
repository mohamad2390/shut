package com.shut.showcase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Portfolio rules: who owns what, and how much one category may hold. */
@Service
public class ShowcaseService {

    public static final int MAX_ITEMS_PER_CATEGORY = 20;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ShowcaseItemRepository repository;

    public ShowcaseService(ShowcaseItemRepository repository) {
        this.repository = repository;
    }

    /** Throws {@link ShowcaseItemRejectedException} if the file is already in the portfolio or the category is full. */
    @Transactional(readOnly = true)
    public void checkCanAdd(Long userId, ShowcaseCategory category, String fileUniqueId) {
        if (repository.existsByUserIdAndFileUniqueId(userId, fileUniqueId)) {
            throw new ShowcaseItemRejectedException(ShowcaseItemRejectedException.Reason.DUPLICATE);
        }
        if (repository.countByUserIdAndCategory(userId, category) >= MAX_ITEMS_PER_CATEGORY) {
            throw new ShowcaseItemRejectedException(ShowcaseItemRejectedException.Reason.LIMIT_REACHED);
        }
    }

    @Transactional
    public ShowcaseItem add(Long userId, ShowcaseCategory category, ShowcaseMediaType mediaType, String fileId,
                            String fileUniqueId, String caption, Long backupMessageId) {
        checkCanAdd(userId, category, fileUniqueId);
        return repository.save(new ShowcaseItem(
                userId, category, mediaType, fileId, fileUniqueId, caption, backupMessageId));
    }

    @Transactional(readOnly = true)
    public Map<ShowcaseCategory, Long> countsByCategory(Long userId) {
        Map<ShowcaseCategory, Long> counts = new EnumMap<>(ShowcaseCategory.class);
        repository.countByCategory(userId).forEach(row -> counts.put(row.getCategory(), row.getTotal()));
        return counts;
    }

    @Transactional(readOnly = true)
    public long count(Long userId, ShowcaseCategory category) {
        return repository.countByUserIdAndCategory(userId, category);
    }

    /** Newest items first. {@code page} starts at 0. */
    @Transactional(readOnly = true)
    public Page<ShowcaseItem> page(Long userId, ShowcaseCategory category, int page, int size) {
        return repository.findByUserIdAndCategory(userId, category, PageRequest.of(page, size, NEWEST_FIRST));
    }

    /** Deletes the item if it belongs to the user, and returns it so callers can clean up related data. */
    @Transactional
    public Optional<ShowcaseItem> deleteOwned(Long itemId, Long userId) {
        return repository.findByIdAndUserId(itemId, userId).map(item -> {
            repository.delete(item);
            return item;
        });
    }
}
