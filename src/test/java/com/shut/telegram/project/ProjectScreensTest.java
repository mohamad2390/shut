package com.shut.telegram.project;

import com.shut.project.Bid;
import com.shut.project.BidStatus;
import com.shut.project.Project;
import com.shut.project.ProjectStatus;
import com.shut.review.RatingSummary;
import com.shut.showcase.ShowcaseCategory;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectScreensTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 11, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final long PROJECT_ID = 10;

    private static Project project() {
        Project project = new Project(1L, "عکاسی عروسی", ShowcaseCategory.PHOTOGRAPHY, "توضیح", "شیراز",
                12_000_000L, NOW, NOW.plusDays(7));
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    private static Bid bid(long id, long amount) {
        Bid bid = new Bid(PROJECT_ID, 2L, amount, "تحویل ده‌روزه", NOW);
        ReflectionTestUtils.setField(bid, "id", id);
        return bid;
    }

    private static List<String> callbacks(Screen screen) {
        return screen.markup().inlineKeyboard().stream().flatMap(List::stream)
                .map(InlineKeyboardButton::callbackData).filter(Objects::nonNull).toList();
    }

    @Test
    void theOwnerSeesTheOffersAndCanCancelAnOpenProject() {
        Screen screen = ProjectScreens.ownerView(project(), true, "۲۷ آبان", 3, null, null, null);

        assertThat(callbacks(screen)).contains(BidCallbacks.list(PROJECT_ID, 0), ProjectCallbacks.cancel(PROJECT_ID));
    }

    @Test
    void theOwnerCannotCancelAnAwardedProject() {
        Project project = project();
        ReflectionTestUtils.setField(project, "status", ProjectStatus.AWARDED);

        Screen screen = ProjectScreens.ownerView(project, false, "۲۷ آبان", 3, "نگار", bid(5, 9_500_000), "negar_ph");

        assertThat(callbacks(screen)).doesNotContain(ProjectCallbacks.cancel(PROJECT_ID));
        assertThat(screen.text()).contains("برنده: نگار");
        assertThat(screen.markup().inlineKeyboard().stream().flatMap(List::stream).map(InlineKeyboardButton::url))
                .contains("https://t.me/negar_ph");
    }

    @Test
    void anotherMemberSeesOnlyTheNumberOfOffersNeverTheirPrices() {
        Screen screen = ProjectScreens.bidderView(project(), "علی", true, "۲۷ آبان", 3, null, null);

        assertThat(screen.text()).contains("تعداد پیشنهادها: ۳");
        assertThat(callbacks(screen)).contains(BidCallbacks.place(PROJECT_ID))
                .doesNotContain(BidCallbacks.list(PROJECT_ID, 0));
    }

    @Test
    void aBidderCanChangeOrWithdrawTheirOfferWhileTheProjectTakesOffers() {
        Screen screen = ProjectScreens.bidderView(project(), "علی", true, "۲۷ آبان", 3, bid(5, 9_500_000), null);

        assertThat(screen.text()).contains("پیشنهاد تو: ۹٬۵۰۰٬۰۰۰ تومان");
        assertThat(callbacks(screen)).contains(BidCallbacks.place(PROJECT_ID), BidCallbacks.withdraw(PROJECT_ID));
    }

    @Test
    void noOfferButtonsAfterTheDeadline() {
        Screen screen = ProjectScreens.bidderView(project(), "علی", false, "۲۷ آبان", 3, bid(5, 9_500_000), null);

        assertThat(callbacks(screen)).doesNotContain(BidCallbacks.place(PROJECT_ID), BidCallbacks.withdraw(PROJECT_ID));
    }

    @Test
    void theWinnerGetsAWayToMessageTheOwner() {
        Bid winner = bid(5, 9_500_000);
        ReflectionTestUtils.setField(winner, "status", BidStatus.ACCEPTED);

        Screen screen = ProjectScreens.bidderView(project(), "علی", false, "۲۷ آبان", 3, winner, "ali_owner");

        assertThat(screen.markup().inlineKeyboard().stream().flatMap(List::stream).map(InlineKeyboardButton::url))
                .contains("https://t.me/ali_owner");
    }

    @Test
    void theOwnersOfferListOnlyOffersToChooseAPendingOfferOfAnOpenProject() {
        Bid pending = bid(5, 9_500_000);
        Screen open = BidScreens.ownerDetail(project(), pending, "نگار", RatingSummary.NONE, null);
        assertThat(callbacks(open)).contains(BidCallbacks.pick(5));

        Project awarded = project();
        ReflectionTestUtils.setField(awarded, "status", ProjectStatus.AWARDED);
        assertThat(callbacks(BidScreens.ownerDetail(awarded, pending, "نگار", RatingSummary.NONE, null)))
                .doesNotContain(BidCallbacks.pick(5));
    }

    @Test
    void offerListShowsPriceNameAndRating() {
        Screen screen = BidScreens.ownerList(project(), new PageImpl<>(List.of(bid(5, 9_500_000)), PageRequest.of(0, 8), 1),
                Map.of(2L, "نگار"), Map.of(2L, new RatingSummary(4.5, 12)));

        assertThat(screen.markup().inlineKeyboard().get(0).get(0).text()).isEqualTo("۹٬۵۰۰٬۰۰۰ تومان — نگار ⭐۴٫۵");
    }

    @Test
    void creationOffersEveryDeadlineAndEveryCategory() {
        assertThat(callbacks(ProjectScreens.askDeadline())).contains(
                ProjectCallbacks.deadline(1), ProjectCallbacks.deadline(3), ProjectCallbacks.deadline(7), ProjectCallbacks.deadline(14));
        assertThat(callbacks(ProjectScreens.askCategory("x")).stream().filter(data -> data.startsWith("project:cat:")))
                .hasSize(ShowcaseCategory.values().length);
    }

    @Test
    void callbackDataFitsTelegramsLimit() {
        long big = Long.MAX_VALUE;
        for (String data : List.of(ProjectCallbacks.open("PHOTO_EDITING", 99999), ProjectCallbacks.mine(99999),
                ProjectCallbacks.view(big), ProjectCallbacks.cancel(big), ProjectCallbacks.confirmCancel(big),
                ProjectCallbacks.category(ShowcaseCategory.PHOTO_EDITING), BidCallbacks.place(big),
                BidCallbacks.confirmWithdraw(big), BidCallbacks.list(big, 99999), BidCallbacks.view(big),
                BidCallbacks.confirmPick(big), BidCallbacks.mine(99999))) {
            assertThat(data.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
        }
    }
}
