package com.shut.telegram.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/** The current conversation step of one Telegram user, with the small amount of data the step needs. */
@Entity
@Table(name = "conversation_states")
public class ConversationState {

    @Id
    @Column(name = "telegram_id")
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConversationStep step;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> data = new HashMap<>();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ConversationState() {
    }

    ConversationState(Long telegramId) {
        this.telegramId = telegramId;
    }

    void restart(ConversationStep step, Map<String, String> data) {
        this.step = step;
        this.data = new HashMap<>(data);
        this.updatedAt = OffsetDateTime.now();
    }

    public ConversationStep getStep() {
        return step;
    }

    public Map<String, String> getData() {
        return data;
    }
}
