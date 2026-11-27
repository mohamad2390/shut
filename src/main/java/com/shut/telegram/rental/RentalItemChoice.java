package com.shut.telegram.rental;

import com.shut.equipment.EquipmentModel;
import com.shut.rental.RentalService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.equipment.EquipmentChoice;
import com.shut.telegram.messaging.Screen;
import com.shut.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;

/** A rental owner picked a product from the equipment list: ask for its daily price, unless it cannot be added. */
@Component
@TelegramBotEnabled
class RentalItemChoice implements EquipmentChoice {

    static final String PURPOSE = "rent";

    private final RentalService rentals;
    private final ConversationService conversations;

    RentalItemChoice(RentalService rentals, ConversationService conversations) {
        this.rentals = rentals;
        this.conversations = conversations;
    }

    @Override
    public String purpose() {
        return PURPOSE;
    }

    @Override
    public String backCallback() {
        return RentalOwnerCallbacks.ITEMS;
    }

    @Override
    public String backLabel() {
        return "↩️ تجهیزات مغازه";
    }

    @Override
    public Outcome chosen(User member, long telegramId, EquipmentModel model) {
        RentalService.ItemCheck check = rentals.checkCanAddItem(member.getId(), model.getId());
        if (check == RentalService.ItemCheck.OK) {
            conversations.begin(telegramId, ConversationStep.AWAITING_RENTAL_ITEM, Map.of(
                    RentalOwnerCallbackHandler.STAGE_KEY, "PRICE",
                    RentalOwnerCallbackHandler.MODEL_KEY, String.valueOf(model.getId())));
            return new Outcome(RentalOwnerScreens.askItemPrice(model), null);
        }
        Screen items = rentals.ownedBy(member.getId())
                .map(shop -> RentalOwnerScreens.items(rentals.items(shop.getId())))
                .orElseGet(RentalOwnerScreens::intro);
        return new Outcome(items, switch (check) {
            case ALREADY_LISTED -> "این تجهیز از قبل توی مغازه‌ات هست.";
            case LIMIT_REACHED -> "به سقف " + RentalService.MAX_ITEMS + " مورد رسیدی. اول یکی رو حذف کن.";
            case NO_PRODUCT -> "این تجهیز دیگه توی فهرست نیست.";
            case NO_SHOP, OK -> null;
        });
    }
}
