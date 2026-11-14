package com.shut.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A product a member owns. */
@Entity
@Table(name = "user_equipment")
public class UserEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "model_id", nullable = false, updatable = false)
    private EquipmentModel model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserEquipment() {
    }

    UserEquipment(Long userId, EquipmentModel model) {
        this.userId = userId;
        this.model = model;
        this.createdAt = OffsetDateTime.now();
    }

    /** The type icon, brand and model, such as "📷 سونی آلفا ۷". */
    public String label() {
        return model.getType().getIcon() + " " + model.displayName();
    }

    /** The label and the type, such as "📷 سونی آلفا ۷ (دوربین عکاسی)". */
    public String describe() {
        return label() + " (" + model.getType().getName() + ")";
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public EquipmentModel getModel() { return model; }
}
