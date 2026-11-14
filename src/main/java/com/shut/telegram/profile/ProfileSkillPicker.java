package com.shut.telegram.profile;

import com.shut.pricing.PricingCatalog;
import com.shut.pricing.ServiceType;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.profile.service.ProfessionalProfileService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets members pick their skills from the service list the price menu uses, which is kept in the database.
 * Every tap is saved at once, so there is nothing to confirm.
 */
@Component
@TelegramBotEnabled
class ProfileSkillPicker {

    static final String LIMIT_NOTE = "به حداکثر تعداد مهارت رسیدی. اول یکی رو برداری.";

    private final TelegramAccountResolver accounts;
    private final ProfessionalProfileService profiles;
    private final PricingCatalog catalog;

    ProfileSkillPicker(TelegramAccountResolver accounts, ProfessionalProfileService profiles, PricingCatalog catalog) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.catalog = catalog;
    }

    Screen screen(TelegramUser from) {
        return screen(from, null);
    }

    /** Ticks or unticks the service, saves, and returns the page as it is now. */
    Screen toggle(TelegramUser from, long serviceId) {
        Long userId = accounts.resolve(from).getId();
        Set<String> skills = new LinkedHashSet<>(currentSkills(userId));
        String note = null;
        ServiceType service = catalog.service(serviceId).filter(ProfileSkillPicker::isChoosable).orElse(null);
        if (service != null && !skills.remove(service.getName())) {
            if (skills.size() >= ProfileSkillScreens.MAX_SKILLS) {
                note = LIMIT_NOTE;
            } else {
                skills.add(service.getName());
            }
        }
        if (service != null && note == null) {
            ProfileDraft draft = new ProfileDraft();
            draft.skills = skills;
            profiles.saveOrUpdate(userId, draft.toRequest());
        }
        return screen(from, note);
    }

    private Screen screen(TelegramUser from, String note) {
        Long userId = accounts.resolve(from).getId();
        return ProfileSkillScreens.build(choices(), currentSkills(userId), note);
    }

    /** The services a member can pick: "other services" is a price-menu entry, not a skill. */
    private List<ServiceType> choices() {
        return catalog.services().stream().filter(ProfileSkillPicker::isChoosable).toList();
    }

    private static boolean isChoosable(ServiceType service) {
        return !service.asksName();
    }

    private Set<String> currentSkills(Long userId) {
        return profiles.findByUserId(userId).map(ProfessionalProfileResponse::skills).orElse(Set.of());
    }
}
