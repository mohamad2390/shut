package com.shut.telegram.project;

import com.shut.project.Bid;
import com.shut.project.BidService;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.project.ProjectStatus;
import com.shut.review.RatingSummary;
import com.shut.review.ReviewService;
import com.shut.showcase.ShowcaseCategory;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Builds the project pages as one user sees them, doing the lookups the screens need. */
@Component
@TelegramBotEnabled
class ProjectPages {

    static final int PAGE_SIZE = 8;

    private final ProjectService projects;
    private final BidService bids;
    private final ReviewService reviews;
    private final MemberNames names;
    private final TelegramAccountResolver accounts;
    private final Clock clock;

    ProjectPages(ProjectService projects, BidService bids, ReviewService reviews, MemberNames names,
                 TelegramAccountResolver accounts, Clock clock) {
        this.projects = projects;
        this.bids = bids;
        this.reviews = reviews;
        this.names = names;
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Open projects the user could bid on. {@code filter} is a category name or "ALL". */
    Screen openList(TelegramUser viewer, String filter, int page) {
        ShowcaseCategory category = ShowcaseCategory.fromKey(filter).orElse(null);
        String effectiveFilter = category == null ? ProjectCallbacks.ALL : category.name();
        return ProjectScreens.openList(effectiveFilter,
                projects.takingBids(viewerId(viewer), category, page, PAGE_SIZE));
    }

    Screen mine(TelegramUser viewer, int page) {
        return ProjectScreens.mineList(projects.ofOwner(viewerId(viewer), page, PAGE_SIZE), projects::isAcceptingBids);
    }

    /** The project's page: the owner's view for the owner, the bidder's view for everyone else. */
    Screen view(TelegramUser viewer, long projectId) {
        Optional<Project> found = projects.find(projectId);
        if (found.isEmpty()) {
            return ProjectScreens.notAvailable();
        }
        Project project = found.get();
        long viewerId = viewerId(viewer);
        boolean takingBids = projects.isAcceptingBids(project);
        String deadline = ProjectText.deadline(project.getClosesAt(), clock.getZone());
        long bidCount = bids.count(project.getId());

        if (project.getOwnerId().equals(viewerId)) {
            Bid winner = project.getAwardedBidId() == null ? null
                    : bids.findForOwner(project.getAwardedBidId(), viewerId).orElse(null);
            return ProjectScreens.ownerView(project, takingBids, deadline, bidCount,
                    winner == null ? null : names.of(winner.getBidderId()), winner,
                    winner == null ? null : names.usernameOf(winner.getBidderId()));
        }
        if (project.getStatus() == ProjectStatus.CANCELLED) {
            return ProjectScreens.notAvailable();
        }
        Bid own = bids.findOwn(project.getId(), viewerId).orElse(null);
        return ProjectScreens.bidderView(project, names.of(project.getOwnerId()), takingBids, deadline, bidCount, own,
                own == null ? null : names.usernameOf(project.getOwnerId()));
    }

    /** The owner's list of offers on a project, cheapest first. */
    Screen bidList(TelegramUser viewer, long projectId, int page) {
        Optional<Project> project = projects.find(projectId);
        Optional<Page<Bid>> found = bids.bidsFor(projectId, viewerId(viewer), page, PAGE_SIZE);
        if (project.isEmpty() || found.isEmpty()) {
            return ProjectScreens.notAvailable();
        }
        Map<Long, String> bidderNames = new HashMap<>();
        Map<Long, RatingSummary> ratings = new HashMap<>();
        found.get().forEach(bid -> {
            bidderNames.put(bid.getBidderId(), names.of(bid.getBidderId()));
            ratings.put(bid.getBidderId(), reviews.summary(bid.getBidderId()));
        });
        return BidScreens.ownerList(project.get(), found.get(), bidderNames, ratings);
    }

    /** One offer, for the owner of its project. */
    Screen bidDetail(TelegramUser viewer, long bidId) {
        Optional<Bid> bid = bids.findForOwner(bidId, viewerId(viewer));
        Optional<Project> project = bid.flatMap(found -> projects.find(found.getProjectId()));
        if (bid.isEmpty() || project.isEmpty()) {
            return ProjectScreens.notAvailable();
        }
        long bidderId = bid.get().getBidderId();
        return BidScreens.ownerDetail(project.get(), bid.get(), names.of(bidderId), reviews.summary(bidderId),
                names.usernameOf(bidderId));
    }

    /** The user's own offers. */
    Screen myBids(TelegramUser viewer, int page) {
        Page<Bid> mine = bids.mine(viewerId(viewer), page, PAGE_SIZE);
        Map<Long, Project> byId = new HashMap<>();
        mine.forEach(bid -> projects.find(bid.getProjectId()).ifPresent(project -> byId.put(project.getId(), project)));
        return BidScreens.myBids(mine, byId);
    }

    private long viewerId(TelegramUser viewer) {
        return accounts.resolve(viewer).getId();
    }
}
