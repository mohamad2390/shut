package com.shut.auth;

import com.shut.user.entity.User;
import com.shut.user.exception.UserNotFoundException;
import com.shut.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Ties a phone number to a Telegram account, so signing in on the web with that number opens the same account the bot
 * uses. If the number already has a web-only account, that account is folded into the Telegram one.
 *
 * <p>A web-only account holds nothing but its sign-ins today. When the web app starts creating data under an account
 * (bookings, reviews), {@link #linkPhone} must move that data too before it deletes the web-only account.
 */
@Service
public class AccountLinkService {

    public enum Result {
        /** The number now belongs to the account. */
        LINKED,
        /** A web-only account with this number existed; its sign-ins now belong to the Telegram account. */
        MERGED,
        /** The account already had this number. */
        ALREADY_LINKED,
        /** The number belongs to another Telegram account. */
        TAKEN
    }

    private final UserRepository users;
    private final WebSessionRepository sessions;

    public AccountLinkService(UserRepository users, WebSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /**
     * @param userId the Telegram account
     * @param phone  normalized, see {@link PhoneNumbers}
     */
    @Transactional
    public Result linkPhone(Long userId, String phone) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (phone.equals(user.getPhoneNumber())) {
            return Result.ALREADY_LINKED;
        }
        Optional<User> owner = users.findByPhoneNumber(phone);
        if (owner.isEmpty()) {
            user.setPhoneNumber(phone);
            return Result.LINKED;
        }
        User other = owner.get();
        if (other.getTelegramId() != null) {
            return Result.TAKEN;
        }
        sessions.moveAll(other.getId(), user.getId());
        users.delete(other);
        // The delete must reach the database before the number is set on the other account: the number is unique.
        users.flush();
        user.setPhoneNumber(phone);
        return Result.MERGED;
    }
}
