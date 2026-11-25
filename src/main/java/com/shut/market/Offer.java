package com.shut.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A buyer's offer on a listing. No amount means they take it at the asking price. */
@Entity
@Table(name = "listing_offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private Long listingId;

    @Column(name = "buyer_id", nullable = false, updatable = false)
    private Long buyerId;

    @Column(updatable = false)
    private Long amount;

    @Column(length = 500, updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OfferStatus status = OfferStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    protected Offer() {
    }

    public Offer(Long listingId, Long buyerId, Long amount, String message) {
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.message = message;
        this.createdAt = OffsetDateTime.now();
    }

    /** Moves a waiting offer to its answer. */
    void answer(OfferStatus answer) {
        this.status = answer;
        this.answeredAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public Long getBuyerId() { return buyerId; }
    public Long getAmount() { return amount; }
    public String getMessage() { return message; }
    public OfferStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
