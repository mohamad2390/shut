package com.shut.studio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** A service package of a studio, such as a full wedding: the services from the catalog it includes, and a price. */
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

    /** The services of the catalog this package brings together, such as photography, video and album. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "studio_offering_services", joinColumns = @JoinColumn(name = "offering_id"))
    @Column(name = "service_id", nullable = false)
    private Set<Long> serviceIds = new LinkedHashSet<>();

    private Long price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StudioOffering() {
    }

    public StudioOffering(Long studioId, String title, Set<Long> serviceIds, Long price) {
        this.studioId = studioId;
        this.title = title;
        this.serviceIds = new LinkedHashSet<>(serviceIds);
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public String getTitle() { return title; }
    public Set<Long> getServiceIds() { return Set.copyOf(serviceIds); }
    /** In toman, or null when the studio gives the price on request. */
    public Long getPrice() { return price; }
}
