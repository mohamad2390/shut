package com.shut.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A sample photo of a venue, kept as a Telegram file id. */
@Entity
@Table(name = "venue_photos")
public class VenuePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    @Column(name = "file_id", nullable = false, length = 512)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, length = 64)
    private String fileUniqueId;

    @Column(name = "backup_message_id")
    private Long backupMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected VenuePhoto() {
    }

    public VenuePhoto(Long venueId, String fileId, String fileUniqueId, Long backupMessageId) {
        this.venueId = venueId;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.backupMessageId = backupMessageId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getVenueId() { return venueId; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public Long getBackupMessageId() { return backupMessageId; }
}
