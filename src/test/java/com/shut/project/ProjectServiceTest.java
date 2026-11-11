package com.shut.project;

import com.shut.showcase.ShowcaseCategory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private static final Long OWNER = 1L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T08:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final BidRepository bids = mock(BidRepository.class);
    private final ProjectService service = new ProjectService(projects, bids, CLOCK);

    private Project create(int days) {
        when(projects.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));
        return service.create(OWNER, "  عکاسی عروسی ", ShowcaseCategory.PHOTOGRAPHY, "یک روز کامل", " شیراز ", 5_000_000L, days);
    }

    @Test
    void createsAnOpenProjectThatClosesAfterTheChosenDays() {
        Project project = create(7);

        assertThat(project.getTitle()).isEqualTo("عکاسی عروسی");
        assertThat(project.getCity()).isEqualTo("شیراز");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(project.getClosesAt()).isEqualTo(NOW.plusDays(7));
        assertThat(project.isAcceptingBids(NOW)).isTrue();
        assertThat(project.isAcceptingBids(NOW.plusDays(7))).isFalse();
    }

    @Test
    void aBlankCityAndNoBudgetAreAllowed() {
        when(projects.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));

        Project project = service.create(OWNER, "تدوین", ShowcaseCategory.VIDEO_EDITING, "کلیپ ۳ دقیقه‌ای", "  ", null, 3);

        assertThat(project.getCity()).isNull();
        assertThat(project.getBudget()).isNull();
    }

    @Test
    void refusesDeadlinesThatAreNotOffered() {
        assertThatThrownBy(() -> create(2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesEmptyOrTooLongTexts() {
        assertThatThrownBy(() -> service.create(OWNER, " ", ShowcaseCategory.OTHER, "x", null, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(OWNER, "x", ShowcaseCategory.OTHER,
                "x".repeat(ProjectService.MAX_DESCRIPTION_LENGTH + 1), null, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesANewProjectWhenTooManyAreOpen() {
        when(projects.countByOwnerIdAndStatus(OWNER, ProjectStatus.OPEN)).thenReturn((long) ProjectService.MAX_OPEN_PROJECTS);

        assertThatThrownBy(() -> create(3)).isInstanceOf(ProjectLimitReachedException.class);
        verify(projects, never()).save(any());
    }

    @Test
    void onlyTheOwnerCanCancelAnOpenProject() {
        Project project = create(3);
        when(projects.findById(10L)).thenReturn(Optional.of(project));

        assertThat(service.cancel(10L, 99L)).isFalse();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(service.cancel(10L, OWNER)).isTrue();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CANCELLED);
        assertThat(service.cancel(10L, OWNER)).isFalse();
    }

    @Test
    void awardingAcceptsOneOfferAndRejectsTheOthers() {
        Project project = create(3);
        Bid winner = bidWithId(5L, 10L, 2L, 900_000);
        Bid other = bidWithId(6L, 10L, 3L, 1_100_000);
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(bids.findById(5L)).thenReturn(Optional.of(winner));
        when(bids.findAllByProjectId(10L)).thenReturn(List.of(winner, other));

        Optional<Award> award = service.award(10L, OWNER, 5L);

        assertThat(award).isPresent();
        assertThat(winner.getStatus()).isEqualTo(BidStatus.ACCEPTED);
        assertThat(other.getStatus()).isEqualTo(BidStatus.REJECTED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.AWARDED);
        assertThat(project.getAwardedBidId()).isEqualTo(5L);
        assertThat(award.get().rejected()).containsExactly(other);
    }

    @Test
    void awardingFailsForSomeoneElsesProjectOrAnOfferOnAnotherProject() {
        Project project = create(3);
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(bids.findById(5L)).thenReturn(Optional.of(bidWithId(5L, 10L, 2L, 900_000)));
        when(bids.findById(6L)).thenReturn(Optional.of(bidWithId(6L, 11L, 2L, 900_000)));

        assertThat(service.award(10L, 99L, 5L)).isEmpty();
        assertThat(service.award(10L, OWNER, 6L)).isEmpty();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
    }

    @Test
    void closedProjectsAreReportedOnce() {
        Project project = create(1);
        when(projects.findByStatusAndClosingNotifiedFalseAndClosesAtLessThanEqual(ProjectStatus.OPEN, NOW))
                .thenReturn(List.of(project));

        assertThat(service.claimClosed()).containsExactly(project);
        assertThat(project.isClosingNotified()).isTrue();
    }

    private static Bid bidWithId(Long id, Long projectId, Long bidderId, long amount) {
        Bid bid = new Bid(projectId, bidderId, amount, null, NOW);
        org.springframework.test.util.ReflectionTestUtils.setField(bid, "id", id);
        return bid;
    }
}
