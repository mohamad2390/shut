package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentModel;
import com.shut.telegram.messaging.Screen;
import com.shut.user.entity.User;

/**
 * A feature that lets a member pick a product (type, brand, model) from the reference list, or add one that is not
 * there yet. The picker is the same for all of them; this says what happens with the product that was picked.
 */
public interface EquipmentChoice {

    /** A short key for the feature, used in callback data and the conversation. */
    String purpose();

    /** Where the "back" button of the type list leads, and what it says. */
    String backCallback();

    String backLabel();

    /**
     * Called when the member picked a product. {@code telegramId} is for features that go on asking questions.
     *
     * @return the screen to show next and, optionally, a short note to send with it
     */
    Outcome chosen(User member, long telegramId, EquipmentModel model);

    /** What to show after a product was picked; {@code note} may be null. */
    record Outcome(Screen screen, String note) {
    }
}
