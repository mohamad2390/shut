package com.shut.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A photo of a rental item, kept as a Telegram file id. */
@Entity
@Table(name = "rental_item_photos")
public class RentalItemPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, updatable = false)
    private Long itemId;

    @Column(name = "file_id", nullable = false, length = 512)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, length = 64)
    private String fileUniqueId;

    @Column(name = "backup_message_id")
    private Long backupMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RentalItemPhoto() {
    }

    public RentalItemPhoto(Long itemId, String fileId, String fileUniqueId, Long backupMessageId) {
        this.itemId = itemId;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.backupMessageId = backupMessageId;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public Long getBackupMessageId() { return backupMessageId; }
}
