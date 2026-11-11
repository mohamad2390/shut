package com.shut.project;

import com.shut.showcase.ShowcaseCategory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BidServiceTest {

    private static final Long OWNER = 1L;
    private static final Long BIDDER = 2L;
    private static final Long PROJECT_ID = 10L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T08:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    private final BidRepository bids = mock(BidRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final BidService service = new BidService(bids, projects, CLOCK);

    private Project projectClosingIn(int days) {
        Project project = new Project(OWNER, "عکاسی", ShowcaseCategory.PHOTOGRAPHY, "توضیح", null, null,
                NOW.minusDays(1), NOW.plusDays(days));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        return project;
    }

    @Test
    void savesAFirstOffer() {
        projectClosingIn(3);
        when(bids.findByProjectIdAndBidderId(PROJECT_ID, BIDDER)).thenReturn(Optional.empty());
        when(bids.save(any(Bid.class))).thenAnswer(call -> call.getArgument(0));

        PlacedBid placed = service.place(PROJECT_ID, BIDDER, 900_000, "  تحویل سه‌روزه ");

        assertThat(placed.revised()).isFalse();
        assertThat(placed.bid().getAmount()).isEqualTo(900_000);
        assertThat(placed.bid().getNote()).isEqualTo("تحویل سه‌روزه");
        assertThat(placed.bid().getStatus()).isEqualTo(BidStatus.PENDING);
    }

    @Test
    void placingAgainReplacesTheEarlierOffer() {
        projectClosingIn(3);
        Bid earlier = new Bid(PROJECT_ID, BIDDER, 900_000, "قدیمی", NOW.minusHours(2));
        when(bids.findByProjectIdAndBidderId(PROJECT_ID, BIDDER)).thenReturn(Optional.of(earlier));

        PlacedBid placed = service.place(PROJECT_ID, BIDDER, 800_000, "  ");

        assertThat(placed.revised()).isTrue();
        assertThat(earlier.getAmount()).isEqualTo(800_000);
        assertThat(earlier.getNote()).isNull();
        verify(bids, never()).save(any());
    }

    @Test
    void refusesAnOfferOnTheBiddersOwnProject() {
        projectClosingIn(3);

        assertThatThrownBy(() -> service.place(PROJECT_ID, OWNER, 900_000, null))
                .isInstanceOfSatisfying(BidRefusedException.class, e -> assertThat(e.reason()).isEqualTo(BidRefusal.OWN_PROJECT));
    }

    @Test
    void refusesAnOfferAfterTheDeadline() {
        projectClosingIn(-1);

        assertThatThrownBy(() -> service.place(PROJECT_ID, BIDDER, 900_000, null))
                .isInstanceOfSatisfying(BidRefusedException.class, e -> assertThat(e.reason()).isEqualTo(BidRefusal.NOT_TAKING_BIDS));
    }

    @Test
    void refusesAnOfferOnAMissingProject() {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.place(PROJECT_ID, BIDDER, 900_000, null))
                .isInstanceOfSatisfying(BidRefusedException.class, e -> assertThat(e.reason()).isEqualTo(BidRefusal.PROJECT_NOT_FOUND));
    }

    @Test
    void refusesBadAmountsAndLongNotes() {
        assertThatThrownBy(() -> service.place(PROJECT_ID, BIDDER, 0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.place(PROJECT_ID, BIDDER, 1, "x".repeat(BidService.MAX_NOTE_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyTheOwnerCanReadTheOffers() {
        projectClosingIn(3);

        assertThat(service.bidsFor(PROJECT_ID, BIDDER, 0, 8)).isEmpty();
        assertThat(service.bidsFor(PROJECT_ID, OWNER, 0, 8)).isPresent();
    }

    @Test
    void anOfferIsOnlyVisibleThroughItsProjectsOwner() {
        projectClosingIn(3);
        Bid bid = new Bid(PROJECT_ID, BIDDER, 900_000, null, NOW);
        when(bids.findById(5L)).thenReturn(Optional.of(bid));

        assertThat(service.findForOwner(5L, BIDDER)).isEmpty();
        assertThat(service.findForOwner(5L, OWNER)).contains(bid);
    }

    @Test
    void withdrawingNeedsAnOpenProjectAndAnOffer() {
        Project project = projectClosingIn(3);
        Bid bid = new Bid(PROJECT_ID, BIDDER, 900_000, null, NOW);
        when(bids.findByProjectIdAndBidderId(PROJECT_ID, BIDDER)).thenReturn(Optional.of(bid));

        assertThat(service.withdraw(PROJECT_ID, BIDDER)).isTrue();
        verify(bids).delete(bid);

        project.cancel();
        assertThat(service.withdraw(PROJECT_ID, BIDDER)).isFalse();
    }
}
