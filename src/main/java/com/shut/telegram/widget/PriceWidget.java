package com.shut.telegram.widget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A price widget message the bot sent, and whose prices it shows. */
@Entity
@Table(name = "price_widgets")
public class PriceWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private long chatId;

    @Column(name = "message_id", nullable = false, updatable = false)
    private long messageId;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private Long providerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PriceWidget() {
    }

    public PriceWidget(long chatId, long messageId, Long providerId) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.providerId = providerId;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public long getMessageId() { return messageId; }
    public Long getProviderId() { return providerId; }
}
