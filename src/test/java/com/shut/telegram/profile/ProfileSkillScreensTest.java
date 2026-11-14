package com.shut.telegram.profile;

import com.shut.pricing.ServiceType;
import com.shut.telegram.messaging.Screen;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileSkillScreensTest {

    private static ServiceType service(long id, String name, String icon) {
        ServiceType service = mock(ServiceType.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        when(service.getIcon()).thenReturn(icon);
        return service;
    }

    private static List<String> labels(Screen screen) {
        return screen.markup().inlineKeyboard().stream().flatMap(List::stream).map(b -> b.text()).toList();
    }

    @Test
    void ownedServicesAreTickedAndTheOthersShowTheirIcon() {
        Screen screen = ProfileSkillScreens.build(
                List.of(service(1, "عکاسی", "📸"), service(2, "تدوین", "✂️")), Set.of("تدوین"), null);

        assertThat(labels(screen)).containsExactly("📸 عکاسی", "✅ تدوین", "✔️ تمام");
    }

    @Test
    void everyServiceButtonTogglesThatService() {
        Screen screen = ProfileSkillScreens.build(List.of(service(7, "عکاسی", "📸")), Set.of(), null);

        assertThat(screen.markup().inlineKeyboard().get(0).get(0).callbackData()).isEqualTo("profile:skill:7");
        assertThat(screen.markup().inlineKeyboard().get(1).get(0).callbackData()).isEqualTo(ProfileCallbacks.DONE);
    }

    @Test
    void aServiceWithoutAnIconGetsTheDefaultOne() {
        Screen screen = ProfileSkillScreens.build(List.of(service(1, "عکاسی", null)), Set.of(), null);

        assertThat(labels(screen).get(0)).isEqualTo("🏷 عکاسی");
    }

    @Test
    void theTextListsAllCurrentSkillsIncludingTypedOnesAndTheNote() {
        Set<String> current = new LinkedHashSet<>(List.of("تدوین", "رتوش حرفه‌ای"));

        Screen screen = ProfileSkillScreens.build(List.of(), current, "یادداشت");

        assertThat(screen.text()).contains("تدوین، رتوش حرفه‌ای").contains("یادداشت");
    }

    @Test
    void anEmptyListSaysNothingIsChosenYet() {
        assertThat(ProfileSkillScreens.build(List.of(), Set.of(), null).text()).contains("هنوز چیزی انتخاب نشده");
    }

    @Test
    void callbackDataStaysUnderTelegramsLimit() {
        assertThat(ProfileCallbacks.skill(Long.MAX_VALUE).getBytes().length).isLessThanOrEqualTo(64);
    }
}
