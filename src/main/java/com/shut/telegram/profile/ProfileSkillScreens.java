package com.shut.telegram.profile;

import com.shut.common.text.PersianDigits;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.ServiceType;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;

import java.util.List;
import java.util.Set;

/** The skills page: the services members can choose from, with the ones they already have ticked. */
final class ProfileSkillScreens {

    static final int MAX_SKILLS = 15;

    private static final String TICK = "✅ ";

    private ProfileSkillScreens() {
    }

    /**
     * @param services the services to choose from
     * @param current  the member's skills, which may include words they typed that are not services
     * @param note     a line to show above the page, or null
     */
    static Screen build(List<ServiceType> services, Set<String> current, String note) {
        StringBuilder text = new StringBuilder(ProfileField.SKILLS.prompt());
        text.append("\n\nحداکثر ").append(PersianDigits.toPersian(MAX_SKILLS)).append(" مهارت.");
        text.append("\n\nمهارت‌های فعلی: ")
                .append(current.isEmpty() ? "هنوز چیزی انتخاب نشده." : String.join("، ", current));
        if (note != null) {
            text.append("\n\n").append(note);
        }
        List<InlineKeyboardButton> buttons = services.stream()
                .map(service -> InlineKeyboardButton.callback(label(service, current), ProfileCallbacks.skill(service.getId())))
                .toList();
        return new Screen(text.toString(), new KeyboardBuilder()
                .grid(buttons, 2)
                .row(InlineKeyboardButton.callback("✔️ تمام", ProfileCallbacks.DONE))
                .build());
    }

    static String label(ServiceType service, Set<String> current) {
        String icon = service.getIcon() == null || service.getIcon().isBlank() ? PricingCatalog.DEFAULT_SERVICE_ICON
                : service.getIcon();
        return current.contains(service.getName()) ? TICK + service.getName() : icon + " " + service.getName();
    }
}
