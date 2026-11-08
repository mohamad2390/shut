package com.shut.telegram.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
}
