package com.shut.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** One price of a venue, such as 3 million per hour, 10 million per day or 50 million for a "private" booking. */
@Entity
@Table(name = "venue_prices")
public class VenuePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    /** What the price is called, or null when the unit says enough. */
    @Column(length = 40)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VenuePriceUnit unit;

    @Column(nullable = false)
    private long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected VenuePrice() {
    }

    public VenuePrice(Long venueId, String title, VenuePriceUnit unit, long amount) {
        this.venueId = venueId;
        this.title = title;
        this.unit = unit;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getVenueId() { return venueId; }
    public String getTitle() { return title; }
    public VenuePriceUnit getUnit() { return unit; }
    public long getAmount() { return amount; }
}
