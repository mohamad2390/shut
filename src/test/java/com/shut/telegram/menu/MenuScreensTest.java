package com.shut.telegram.menu;

import com.shut.telegram.browse.BrowseCallbacks;
import com.shut.telegram.venue.VenueCallbacks;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.project.ProjectCallbacks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuScreensTest {

    @Test
    void mainMenuHasThePersonalSectionAndTheSharedOnes() {
        assertThat(callbacks(MainMenuScreen.build("منوی اصلی")))
                .containsExactly(MenuCallbacks.ME, BrowseCallbacks.START, ProjectCallbacks.HOME, VenueCallbacks.HOME);
    }

    @Test
    void specialistSeesTheFourToolsTheLinkAndNoRegistration() {
        assertThat(callbacks(MyInfoScreen.forSpecialist())).containsExactly(MenuCallbacks.PROFILE,
                MenuCallbacks.CALENDAR, MenuCallbacks.SHOWCASE, MenuCallbacks.PRICING, MenuCallbacks.LINK, MenuCallbacks.HOME);
    }

    @Test
    void customerSeesRegistrationOnly() {
        assertThat(callbacks(MyInfoScreen.forCustomer()))
                .containsExactly(MenuCallbacks.REGISTER, MenuCallbacks.HOME);
    }

    private static List<String> callbacks(Screen screen) {
        return screen.markup().inlineKeyboard().stream().flatMap(List::stream)
                .map(button -> button.callbackData()).toList();
    }
}
