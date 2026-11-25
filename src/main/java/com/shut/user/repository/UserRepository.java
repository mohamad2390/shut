package com.shut.user.repository;

import com.shut.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByTelegramUsernameIgnoreCase(String telegramUsername);

    boolean existsByTelegramId(Long telegramId);

    /**
     * Accounts whose name, Telegram username or phone contains the text, newest first.
     *
     * @param pattern lower case, already wrapped in {@code %}
     */
    @Query("""
            select u from User u
            where lower(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))) like :pattern
               or lower(coalesce(u.telegramUsername, '')) like :pattern
               or coalesce(u.phoneNumber, '') like :pattern
            order by u.id desc
            """)
    Page<User> search(@Param("pattern") String pattern, Pageable pageable);
}
