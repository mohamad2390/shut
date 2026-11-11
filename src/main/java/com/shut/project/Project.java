package com.shut.project;

import com.shut.showcase.ShowcaseCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Work a member wants done, open for offers from other members until its deadline. */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowcaseCategory category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 100)
    private String city;

    /** What the owner plans to spend, in tomans. Null if they chose not to say. */
    private Long budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closes_at", nullable = false, updatable = false)
    private OffsetDateTime closesAt;

    /** Set once the owner has been told that the deadline passed. */
    @Column(name = "closing_notified", nullable = false)
    private boolean closingNotified;

    @Column(name = "awarded_bid_id")
    private Long awardedBidId;

    protected Project() {
    }

    public Project(Long ownerId, String title, ShowcaseCategory category, String description, String city,
                   Long budget, OffsetDateTime createdAt, OffsetDateTime closesAt) {
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.city = city;
        this.budget = budget;
        this.status = ProjectStatus.OPEN;
        this.createdAt = createdAt;
        this.closesAt = closesAt;
    }

    /** True while the project is open and its deadline has not passed. */
    public boolean isAcceptingBids(OffsetDateTime now) {
        return status == ProjectStatus.OPEN && now.isBefore(closesAt);
    }

    void award(Long bidId) {
        this.status = ProjectStatus.AWARDED;
        this.awardedBidId = bidId;
    }

    void cancel() {
        this.status = ProjectStatus.CANCELLED;
    }

    void markClosingNotified() {
        this.closingNotified = true;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public ShowcaseCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getCity() { return city; }
    public Long getBudget() { return budget; }
    public ProjectStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getClosesAt() { return closesAt; }
    public boolean isClosingNotified() { return closingNotified; }
    public Long getAwardedBidId() { return awardedBidId; }
}
