package com.shut.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** One service on a provider's price menu, for example "photography, 1,000,000 tomans per hour". */
@Entity
@Table(name = "price_items")
public class PriceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingUnit unit;

    /** Price in tomans. */
    @Column(nullable = false)
    private long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PriceItem() {
    }

    public PriceItem(Long userId, String title, PricingUnit unit, long amount) {
        this.userId = userId;
        this.title = title;
        this.unit = unit;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public PricingUnit getUnit() { return unit; }
    public long getAmount() { return amount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
