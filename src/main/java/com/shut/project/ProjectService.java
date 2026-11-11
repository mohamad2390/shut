package com.shut.project;

import com.shut.showcase.ShowcaseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Projects members post to collect offers. Offers are accepted until the project's deadline; after that the owner
 * can still choose one, until they cancel the project.
 */
@Service
public class ProjectService {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_CITY_LENGTH = 100;
    public static final int MAX_OPEN_PROJECTS = 10;
    /** How many days a project can take offers for. */
    public static final List<Integer> DEADLINE_DAYS = List.of(1, 3, 7, 14);

    private final ProjectRepository projects;
    private final BidRepository bids;
    private final Clock clock;

    public ProjectService(ProjectRepository projects, BidRepository bids, Clock clock) {
        this.projects = projects;
        this.bids = bids;
        this.clock = clock;
    }

    /**
     * @param city   null for work that does not depend on a place
     * @param budget in tomans; null if the owner does not want to say
     * @throws ProjectLimitReachedException if the owner has too many open projects
     * @throws IllegalArgumentException     if a text is empty or too long, the budget is not positive or
     *                                      the deadline is not one of {@link #DEADLINE_DAYS}
     */
    @Transactional
    public Project create(Long ownerId, String title, ShowcaseCategory category, String description, String city,
                          Long budget, int deadlineDays) {
        String cleanTitle = required(title, MAX_TITLE_LENGTH, "title");
        String cleanDescription = required(description, MAX_DESCRIPTION_LENGTH, "description");
        String cleanCity = optional(city, MAX_CITY_LENGTH, "city");
        if (budget != null && budget <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }
        if (!DEADLINE_DAYS.contains(deadlineDays)) {
            throw new IllegalArgumentException("Deadline must be one of " + DEADLINE_DAYS + " days");
        }
        if (projects.countByOwnerIdAndStatus(ownerId, ProjectStatus.OPEN) >= MAX_OPEN_PROJECTS) {
            throw new ProjectLimitReachedException();
        }
        OffsetDateTime now = now();
        return projects.save(new Project(ownerId, cleanTitle, category, cleanDescription, cleanCity, budget,
                now, now.plusDays(deadlineDays)));
    }

    @Transactional(readOnly = true)
    public Optional<Project> find(Long projectId) {
        return projects.findById(projectId);
    }

    /** Projects that still take offers, soonest deadline first. The viewer's own projects are left out. */
    @Transactional(readOnly = true)
    public Page<Project> takingBids(Long viewerId, ShowcaseCategory category, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(0, page), size,
                Sort.by(Sort.Order.asc("closesAt"), Sort.Order.asc("id")));
        return category == null
                ? projects.findTakingBids(now(), viewerId, request)
                : projects.findTakingBidsInCategory(now(), viewerId, category, request);
    }

    /** The member's own projects, newest first. */
    @Transactional(readOnly = true)
    public Page<Project> ofOwner(Long ownerId, int page, int size) {
        return projects.findByOwnerId(ownerId, PageRequest.of(Math.max(0, page), size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    public boolean isAcceptingBids(Project project) {
        return project.isAcceptingBids(now());
    }

    /** Withdraws an open project. Returns false if it is not the owner's or is no longer open. */
    @Transactional
    public boolean cancel(Long projectId, Long ownerId) {
        return projects.findById(projectId)
                .filter(project -> project.getOwnerId().equals(ownerId) && project.getStatus() == ProjectStatus.OPEN)
                .map(project -> {
                    project.cancel();
                    return true;
                })
                .orElse(false);
    }

    /**
     * Chooses an offer. The chosen offer is accepted and every other offer on the project is rejected.
     * Returns empty if the project is not the owner's, is not open, or the offer is not a pending offer on it.
     */
    @Transactional
    public Optional<Award> award(Long projectId, Long ownerId, Long bidId) {
        Optional<Project> project = projects.findById(projectId)
                .filter(found -> found.getOwnerId().equals(ownerId) && found.getStatus() == ProjectStatus.OPEN);
        Optional<Bid> winner = bids.findById(bidId)
                .filter(bid -> bid.getProjectId().equals(projectId) && bid.getStatus() == BidStatus.PENDING);
        if (project.isEmpty() || winner.isEmpty()) {
            return Optional.empty();
        }

        winner.get().accept();
        List<Bid> rejected = bids.findAllByProjectId(projectId).stream()
                .filter(bid -> !bid.getId().equals(bidId))
                .toList();
        rejected.forEach(Bid::reject);
        project.get().award(bidId);
        return Optional.of(new Award(project.get(), winner.get(), rejected));
    }

    /**
     * Open projects whose deadline has passed and whose owner has not been told yet. Each project is returned
     * once: it is marked as told here.
     */
    @Transactional
    public List<Project> claimClosed() {
        List<Project> closed = projects.findByStatusAndClosingNotifiedFalseAndClosesAtLessThanEqual(
                ProjectStatus.OPEN, now());
        closed.forEach(Project::markClosingNotified);
        return closed;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String required(String value, int maxLength, String field) {
        String cleaned = optional(value, maxLength, field);
        if (cleaned == null) {
            throw new IllegalArgumentException("The " + field + " is required");
        }
        return cleaned;
    }

    private static String optional(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.strip();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException("The " + field + " is longer than " + maxLength + " characters");
        }
        return cleaned;
    }
}
