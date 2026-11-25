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

/** A piece of gear a member sells. */
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(length = 60)
    private String brand;

    @Column(length = 80)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ListingCondition condition;

    /** In tomans. */
    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private boolean negotiable = true;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ListingStatus status = ListingStatus.ACTIVE;

    /** Set by an administrator; a hidden listing is shown to nobody but its seller. */
    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Listing() {
    }

    public Listing(Long sellerId, String title, ListingCondition condition, long price) {
        this.sellerId = sellerId;
        this.title = title;
        this.condition = condition;
        this.price = price;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    /** Shown to others: active and not hidden. */
    public boolean isPublic() {
        return status == ListingStatus.ACTIVE && !hidden;
    }

    public Long getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; touch(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; touch(); }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; touch(); }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; touch(); }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; touch(); }
    public ListingCondition getCondition() { return condition; }
    public void setCondition(ListingCondition condition) { this.condition = condition; touch(); }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; touch(); }
    public boolean isNegotiable() { return negotiable; }
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; touch(); }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; touch(); }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; touch(); }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
