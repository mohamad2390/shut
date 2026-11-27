package com.shut.rental;

import com.shut.equipment.EquipmentModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A product from the equipment catalog (type, brand, model) that a shop rents out, with its price per day. */
@Entity
@Table(name = "rental_items")
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "model_id", nullable = false, updatable = false)
    private EquipmentModel model;

    @Column(length = 500)
    private String description;

    @Column(name = "daily_price", nullable = false)
    private long dailyPrice;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RentalItem() {
    }

    public RentalItem(Long shopId, EquipmentModel model, String description, long dailyPrice) {
        this.shopId = shopId;
        this.model = model;
        this.description = description;
        this.dailyPrice = dailyPrice;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getShopId() { return shopId; }
    public EquipmentModel getModel() { return model; }
    /** The brand and model, such as "سونی آلفا ۷". */
    public String getTitle() { return model.displayName(); }
    public String getDescription() { return description; }
    public long getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(long dailyPrice) { this.dailyPrice = dailyPrice; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
