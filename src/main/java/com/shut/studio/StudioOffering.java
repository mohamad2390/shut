package com.shut.studio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A service package of a studio, such as a full wedding, with what it includes and an optional price. */
@Entity
@Table(name = "studio_offerings")
public class StudioOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false, updatable = false)
    private Long studioId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String includes;

    private Long price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StudioOffering() {
    }

    public StudioOffering(Long studioId, String title, String includes, Long price) {
        this.studioId = studioId;
        this.title = title;
        this.includes = includes;
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public String getTitle() { return title; }
    public String getIncludes() { return includes; }
    /** In toman, or null when the studio gives the price on request. */
    public Long getPrice() { return price; }
}
