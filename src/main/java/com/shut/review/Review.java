package com.shut.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A member's rating of another member, with an optional comment. Each reviewer has at most one per provider. */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private Long providerId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private Long reviewerId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Review() {
    }

    public Review(Long providerId, Long reviewerId, int rating) {
        this.providerId = providerId;
        this.reviewerId = reviewerId;
        this.rating = rating;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void changeRating(int rating) {
        this.rating = rating;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Null removes the comment. */
    public void changeComment(String comment) {
        this.comment = comment;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProviderId() { return providerId; }
    public Long getReviewerId() { return reviewerId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
