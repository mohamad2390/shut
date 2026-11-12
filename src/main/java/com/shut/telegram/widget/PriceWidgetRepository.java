package com.shut.telegram.widget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceWidgetRepository extends JpaRepository<PriceWidget, Long> {

    Optional<PriceWidget> findByChatIdAndMessageId(long chatId, long messageId);

    /** The price widgets of one member's prices that were sent to a chat. */
    List<PriceWidget> findByChatIdAndProviderId(long chatId, Long providerId);
}
