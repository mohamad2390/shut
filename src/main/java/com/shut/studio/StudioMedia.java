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

/** One photo or video of the studio's showcase, kept as a Telegram file id. */
@Entity
@Table(name = "studio_media")
public class StudioMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false, updatable = false)
    private Long studioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaKind kind;

    @Column(name = "file_id", nullable = false, length = 512)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, length = 64)
    private String fileUniqueId;

    @Column(name = "backup_message_id")
    private Long backupMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StudioMedia() {
    }

    public StudioMedia(Long studioId, MediaKind kind, String fileId, String fileUniqueId, Long backupMessageId) {
        this.studioId = studioId;
        this.kind = kind;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.backupMessageId = backupMessageId;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public MediaKind getKind() { return kind; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public Long getBackupMessageId() { return backupMessageId; }
}
