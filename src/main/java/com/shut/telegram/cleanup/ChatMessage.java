package com.shut.telegram.cleanup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** A message in a chat, from the bot or from the user, remembered so that it can be deleted later. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private long chatId;

    @Column(name = "message_id", nullable = false, updatable = false)
    private long messageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(long chatId, long messageId) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.createdAt = OffsetDateTime.now();
    }

    public long getChatId() { return chatId; }
    public long getMessageId() { return messageId; }
}
