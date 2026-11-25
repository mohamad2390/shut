package com.shut.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** The basic card of a studio or an equipment rental. */
@Entity
@Table(name = "business_profiles")
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessKind kind;

    @Column(nullable = false)
    private String name;

    private String city;

    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected BusinessProfile() {
    }

    public BusinessProfile(Long userId, BusinessKind kind, String name, String city, String description) {
        this.userId = userId;
        this.kind = kind;
        this.name = name;
        this.city = city;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void change(String name, String city, String description) {
        this.name = name;
        this.city = city;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BusinessKind getKind() { return kind; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
}
