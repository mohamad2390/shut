package com.shut.telegram.widget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WidgetMessageRepository extends JpaRepository<WidgetMessage, Long> {

    Optional<WidgetMessage> findByChatIdAndMessageId(long chatId, long messageId);

    /** The widgets of one kind about one member that were sent to a chat. */
    List<WidgetMessage> findByChatIdAndProviderIdAndKind(long chatId, Long providerId, WidgetKind kind);
}
