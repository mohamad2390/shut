package com.shut.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebSessionRepository extends JpaRepository<WebSession, Long> {

    Optional<WebSession> findByTokenHash(String tokenHash);

    /** Ends every sign-in of one account. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WebSession s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /** Hands every sign-in of one account to another; used when two accounts of the same person become one. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update WebSession s set s.userId = :to where s.userId = :from")
    int moveAll(@Param("from") Long from, @Param("to") Long to);
}
