package com.shut.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A photo of the gear, kept as a Telegram file id. */
@Entity
@Table(name = "listing_photos")
public class ListingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private Long listingId;

    @Column(name = "file_id", nullable = false, length = 512)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, length = 64)
    private String fileUniqueId;

    @Column(name = "backup_message_id")
    private Long backupMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ListingPhoto() {
    }

    public ListingPhoto(Long listingId, String fileId, String fileUniqueId, Long backupMessageId) {
        this.listingId = listingId;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.backupMessageId = backupMessageId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public Long getBackupMessageId() { return backupMessageId; }
}
