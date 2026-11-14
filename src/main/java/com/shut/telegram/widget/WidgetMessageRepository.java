package com.shut.telegram.widget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WidgetMessageRepository extends JpaRepository<WidgetMessage, Long> {

    Optional<WidgetMessage> findByChatIdAndMessageId(long chatId, long messageId);

    /** The widgets of one kind about one member that were sent to a chat. */
    List<WidgetMessage> findByChatIdAndProviderIdAndKind(long chatId, Long providerId, WidgetKind kind);

    @Modifying
    @Transactional
    @Query("delete from WidgetMessage w where w.chatId = :chatId")
    void deleteByChatId(@Param("chatId") long chatId);

    /** The widgets of any of the kinds that were sent to a chat. */
    List<WidgetMessage> findByChatIdAndKindIn(long chatId, java.util.Collection<WidgetKind> kinds);

    /** The widgets of one kind that are not about a member, such as the ranked list, sent to a chat. */
    List<WidgetMessage> findByChatIdAndProviderIdIsNullAndKind(long chatId, WidgetKind kind);
}
