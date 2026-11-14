package com.shut.equipment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The gear each member owns. */
@Service
public class EquipmentService {

    public static final int MAX_ITEMS = 30;

    /** What happened when a member added a product. */
    public enum AddResult { ADDED, ALREADY_OWNED, LIMIT_REACHED }

    private final UserEquipmentRepository owned;
    private final EquipmentModelRepository models;

    public EquipmentService(UserEquipmentRepository owned, EquipmentModelRepository models) {
        this.owned = owned;
        this.models = models;
    }

    @Transactional(readOnly = true)
    public List<UserEquipment> list(long userId) {
        return owned.findByUserIdOrderByIdAsc(userId);
    }

    /** Each product as a short line, such as "📷 سونی آلفا ۷", for the profile card. */
    @Transactional(readOnly = true)
    public List<String> lines(long userId) {
        return list(userId).stream().map(UserEquipment::label).toList();
    }

    /** @throws IllegalArgumentException if the product does not exist */
    @Transactional
    public AddResult add(long userId, long modelId) {
        EquipmentModel model = models.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("No equipment model " + modelId));
        if (owned.existsByUserIdAndModel_Id(userId, modelId)) {
            return AddResult.ALREADY_OWNED;
        }
        if (owned.countByUserId(userId) >= MAX_ITEMS) {
            return AddResult.LIMIT_REACHED;
        }
        owned.save(new UserEquipment(userId, model));
        return AddResult.ADDED;
    }

    /** Only the member's own gear can be removed. Returns false if there was nothing to remove. */
    @Transactional
    public boolean remove(long userId, long userEquipmentId) {
        return owned.findByIdAndUserId(userEquipmentId, userId).map(item -> {
            owned.delete(item);
            return true;
        }).orElse(false);
    }
}
