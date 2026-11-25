package com.shut.studio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A specialist in a studio's people list; pending until they accept the invitation. */
@Entity
@Table(name = "studio_members")
public class StudioMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false, updatable = false)
    private Long studioId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StudioMember() {
    }

    public StudioMember(Long studioId, Long userId) {
        this.studioId = studioId;
        this.userId = userId;
        this.status = MemberStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public void accept() {
        this.status = MemberStatus.ACCEPTED;
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public Long getUserId() { return userId; }
    public MemberStatus getStatus() { return status; }
}
