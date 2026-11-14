package com.shut.telegram.cleanup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** The ids of the messages remembered for a chat, newest first. */
    @Query("select m.messageId from ChatMessage m where m.chatId = :chatId order by m.messageId desc")
    List<Long> messageIds(@Param("chatId") long chatId);

    @Modifying
    @Transactional
    @Query("delete from ChatMessage m where m.chatId = :chatId")
    int deleteAllOf(@Param("chatId") long chatId);

    @Modifying
    @Transactional
    @Query("delete from ChatMessage m where m.createdAt < :before")
    int deleteOlderThan(@Param("before") OffsetDateTime before);
}
