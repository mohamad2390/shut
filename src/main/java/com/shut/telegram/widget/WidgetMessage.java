package com.shut.telegram.widget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A widget message the bot sent, and which member's prices or calendar it shows. */
@Entity
@Table(name = "widget_messages")
public class WidgetMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private long chatId;

    @Column(name = "message_id", nullable = false, updatable = false)
    private long messageId;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private Long providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private WidgetKind kind;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected WidgetMessage() {
    }

    public WidgetMessage(long chatId, long messageId, Long providerId, WidgetKind kind) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.providerId = providerId;
        this.kind = kind;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public long getMessageId() { return messageId; }
    public Long getProviderId() { return providerId; }
    public WidgetKind getKind() { return kind; }
}
