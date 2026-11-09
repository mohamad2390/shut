package com.shut.showcase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** One photo, video or file in a user's portfolio, referenced by its Telegram file id. */
@Entity
@Table(name = "showcase_items")
public class ShowcaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShowcaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private ShowcaseMediaType mediaType;

    @Column(name = "file_id", nullable = false, length = 512)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, length = 64)
    private String fileUniqueId;

    @Column(length = 1024)
    private String caption;

    /** Id of the copy kept in the backup channel, or null when no backup was made. */
    @Column(name = "backup_message_id")
    private Long backupMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ShowcaseItem() {
    }

    public ShowcaseItem(Long userId, ShowcaseCategory category, ShowcaseMediaType mediaType, String fileId,
                        String fileUniqueId, String caption, Long backupMessageId) {
        this.userId = userId;
        this.category = category;
        this.mediaType = mediaType;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.caption = caption;
        this.backupMessageId = backupMessageId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ShowcaseCategory getCategory() { return category; }
    public ShowcaseMediaType getMediaType() { return mediaType; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public String getCaption() { return caption; }
    public Long getBackupMessageId() { return backupMessageId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
