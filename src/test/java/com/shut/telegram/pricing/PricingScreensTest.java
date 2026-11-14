package com.shut.telegram.pricing;

import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingUnit;
import com.shut.pricing.ServiceType;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PricingScreensTest {

    private static final Map<PricingUnit, String> ICONS = new EnumMap<>(Map.of(
            PricingUnit.HOUR, "⏱", PricingUnit.DAY, "📅", PricingUnit.PHOTO, "📷",
            PricingUnit.VIDEO_MINUTE, "🎬", PricingUnit.PROJECT, "📦"));

    private static ServiceType service(long id, String name, String icon) {
        ServiceType service = new ServiceType(name, icon);
        ReflectionTestUtils.setField(service, "id", id);
        return service;
    }

    private static List<InlineKeyboardButton> buttons(Screen screen) {
        return screen.markup().inlineKeyboard().stream().flatMap(List::stream).toList();
    }

    @Test
    void theServiceListHasOneButtonPerServiceWithItsIconAndACancelButton() {
        Screen screen = PricingScreens.askService(List.of(service(4, "عکاسی عروسی", "💍"), service(9, "تدوین", "✂️")));

        List<InlineKeyboardButton> buttons = buttons(screen);

        assertThat(buttons).extracting(InlineKeyboardButton::text)
                .containsExactly("💍 عکاسی عروسی", "✂️ تدوین", "انصراف");
        assertThat(buttons).extracting(InlineKeyboardButton::callbackData)
                .containsExactly("pricing:svc:4", "pricing:svc:9", PricingCallbacks.CANCEL);
    }

    @Test
    void theUnitButtonsShowTheIconsFromTheTable() {
        Screen screen = PricingScreens.askUnit(service(4, "عکاسی عروسی", "💍"), "عکاسی عروسی", ICONS);

        assertThat(screen.text()).contains("💍 عکاسی عروسی");
        assertThat(buttons(screen)).extracting(InlineKeyboardButton::text)
                .contains("⏱ " + PricingUnit.HOUR.phrase(), "📦 " + PricingUnit.PROJECT.phrase());
    }

    @Test
    void theNameOfThePriceIsShownWhenTheMemberGaveOne() {
        Screen screen = PricingScreens.askUnit(service(1, "سایر خدمات", "🧩"), "عکاسی از غذا", ICONS);

        assertThat(screen.text()).contains("🧩 عکاسی از غذا").doesNotContain("سایر خدمات");
    }

    @Test
    void askingForANameCanBeSkippedOrCancelled() {
        Screen screen = PricingScreens.askName(service(1, "سایر خدمات", "🧩"));

        assertThat(buttons(screen)).extracting(InlineKeyboardButton::callbackData)
                .containsExactly(PricingCallbacks.SKIP_NAME, PricingCallbacks.CANCEL);
    }

    @Test
    void aUnitWithoutAnIconStillGetsAButton() {
        assertThat(PricingScreens.unitLabel(PricingUnit.DAY, Map.of())).startsWith("💰 ");
    }

    @Test
    void thePriceLinesStartWithTheIconOfTheService() {
        PriceItem item = new PriceItem(1L, service(4, "عکاسی عروسی", "💍"), PricingUnit.HOUR, 1_000_000);

        assertThat(PriceText.line(item)).startsWith("💍 عکاسی عروسی:");
        assertThat(PricingScreens.list(List.of(item)).text()).contains("💍 عکاسی عروسی");
    }
}
