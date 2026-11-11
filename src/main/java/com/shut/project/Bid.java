package com.shut.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A provider's price offer on a project. */
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "bidder_id", nullable = false, updatable = false)
    private Long bidderId;

    /** The offered price in tomans. */
    @Column(nullable = false)
    private long amount;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BidStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Bid() {
    }

    public Bid(Long projectId, Long bidderId, long amount, String note, OffsetDateTime now) {
        this.projectId = projectId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.note = note;
        this.status = BidStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void revise(long amount, String note, OffsetDateTime now) {
        this.amount = amount;
        this.note = note;
        this.updatedAt = now;
    }

    void accept() {
        this.status = BidStatus.ACCEPTED;
    }

    void reject() {
        this.status = BidStatus.REJECTED;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getBidderId() { return bidderId; }
    public long getAmount() { return amount; }
    public String getNote() { return note; }
    public BidStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
