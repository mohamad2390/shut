package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.user.entity.User;
import org.springframework.stereotype.Component;

/** Adds the picked product to the gear the member owns, shown on their profile. */
@Component
@TelegramBotEnabled
class OwnGearChoice implements EquipmentChoice {

    static final String PURPOSE = "own";

    private final EquipmentService equipment;

    OwnGearChoice(EquipmentService equipment) {
        this.equipment = equipment;
    }

    @Override
    public String purpose() {
        return PURPOSE;
    }

    @Override
    public String backCallback() {
        return EquipmentCallbacks.HOME;
    }

    @Override
    public String backLabel() {
        return "↩️ تجهیزات من";
    }

    @Override
    public Outcome chosen(User member, long telegramId, EquipmentModel model) {
        EquipmentService.AddResult result = equipment.add(member.getId(), model.getId());
        String note = switch (result) {
            case ADDED -> "✅ " + model.displayName() + " به تجهیزاتت اضافه شد.";
            case ALREADY_OWNED -> "این تجهیز از قبل توی فهرستت هست.";
            case LIMIT_REACHED -> "حداکثر " + EquipmentService.MAX_ITEMS + " تجهیز می‌تونی داشته باشی. اول یکی رو حذف کن.";
        };
        return new Outcome(EquipmentScreens.home(equipment.list(member.getId())), note);
    }
}
