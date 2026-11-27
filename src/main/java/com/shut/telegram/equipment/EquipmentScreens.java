package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentBrand;
import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentType;
import com.shut.equipment.UserEquipment;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/** The pages for the gear a member owns and for adding more: type, then brand, then model. */
final class EquipmentScreens {

    private EquipmentScreens() {
    }

    static Screen home(List<UserEquipment> items) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (items.isEmpty()) {
            text = "🎒 هنوز تجهیزی اضافه نکرده‌ای.\n\nنوع، برند و مدل تجهیزت رو انتخاب کن؛ "
                    + "اگه توی فهرست نبود، خودت اضافه‌ش کن.";
        } else {
            StringBuilder lines = new StringBuilder("🎒 تجهیزات تو\n");
            items.forEach(item -> lines.append("\n").append(item.describe()));
            lines.append("\n\nبرای حذف هر مورد، دکمه‌ی ✖ کنارش رو بزن.");
            text = lines.toString();
            items.forEach(item -> keyboard.row(InlineKeyboardButton.callback(
                    "✖ " + item.label(), EquipmentCallbacks.remove(item.getId()))));
        }
        keyboard.row(InlineKeyboardButton.callback("➕ افزودن تجهیز", EquipmentCallbacks.choose(OwnGearChoice.PURPOSE)))
                .row(InlineKeyboardButton.callback("↩️ پروفایل", MenuCallbacks.PROFILE));
        return new Screen(text, keyboard.build());
    }

    static Screen chooseType(String purpose, List<EquipmentType> types, String backCallback, String backLabel) {
        List<InlineKeyboardButton> buttons = new ArrayList<>(types.stream()
                .map(type -> InlineKeyboardButton.callback(type.getIcon() + " " + type.getName(),
                        EquipmentCallbacks.type(purpose, type.getId())))
                .toList());
        return new Screen("🎒 نوع تجهیز چیه؟",
                new KeyboardBuilder()
                        .grid(buttons, 2)
                        .row(InlineKeyboardButton.callback("➕ نوع دیگه", EquipmentCallbacks.otherType(purpose)))
                        .row(InlineKeyboardButton.callback(backLabel, backCallback))
                        .build());
    }

    static Screen askType() {
        return typed("نوع تجهیزت رو بنویس (مثلاً «رِف‌لکتور»).");
    }

    static Screen chooseBrand(String purpose, EquipmentType type, List<EquipmentBrand> brands) {
        List<InlineKeyboardButton> buttons = brands.stream()
                .map(brand -> InlineKeyboardButton.callback(brand.getName(),
                        EquipmentCallbacks.brand(purpose, type.getId(), brand.getId())))
                .toList();
        return new Screen(type.getIcon() + " " + type.getName() + "\n\nبرندش چیه؟",
                new KeyboardBuilder()
                        .grid(buttons, 3)
                        .row(InlineKeyboardButton.callback("➕ برند دیگه", EquipmentCallbacks.otherBrand(purpose, type.getId())))
                        .row(InlineKeyboardButton.callback("↩️ نوع‌ها", EquipmentCallbacks.choose(purpose)))
                        .build());
    }

    static Screen askBrand(EquipmentType type) {
        return typed("برند " + type.getName() + " رو بنویس (مثلاً «سونی» یا Sony).");
    }

    static Screen chooseModel(String purpose, EquipmentType type, EquipmentBrand brand, List<EquipmentModel> models) {
        List<InlineKeyboardButton> buttons = models.stream()
                .map(model -> InlineKeyboardButton.callback(model.getName(), EquipmentCallbacks.model(purpose, model.getId())))
                .toList();
        String text = type.getIcon() + " " + type.getName() + " " + brand.getName()
                + (models.isEmpty() ? "\n\nهنوز مدلی ثبت نشده. اولین نفر باش و مدلش رو بنویس." : "\n\nمدلش چیه؟");
        return new Screen(text,
                new KeyboardBuilder()
                        .grid(buttons, 2)
                        .row(InlineKeyboardButton.callback("➕ مدل دیگه",
                                EquipmentCallbacks.otherModel(purpose, type.getId(), brand.getId())))
                        .row(InlineKeyboardButton.callback("↩️ برندها", EquipmentCallbacks.type(purpose, type.getId())))
                        .build());
    }

    static Screen askModel(EquipmentType type, EquipmentBrand brand) {
        return typed("مدل " + type.getName() + " " + brand.getName() + " رو بنویس (مثلاً «آلفا ۷»).");
    }

    static Screen gone() {
        return new Screen("این مورد دیگه وجود نداره.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("↩️ تجهیزات من", EquipmentCallbacks.HOME))
                        .build());
    }

    private static Screen typed(String prompt) {
        return new Screen(prompt + "\n\nبرای انصراف /cancel رو بزن.", null);
    }
}
