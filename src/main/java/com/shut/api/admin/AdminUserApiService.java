package com.shut.api.admin;

import com.shut.api.BadRequestException;
import com.shut.api.ConflictException;
import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.auth.SessionService;
import com.shut.common.text.PersianDigits;
import com.shut.user.entity.User;
import com.shut.user.exception.UserNotFoundException;
import com.shut.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looking up accounts and blocking them. A blocked account cannot sign in on the web, its sign-ins end at once, and
 * it no longer shows up in the specialist list. (The bot does not check this flag.)
 */
@Service
public class AdminUserApiService {

    private final UserRepository users;
    private final SessionService sessions;

    public AdminUserApiService(UserRepository users, SessionService sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /** @param q part of a name, Telegram username or phone number; empty lists everyone */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserItem> list(String q, int page, int size) {
        String text = q == null ? "" : PersianDigits.toAscii(q).strip().toLowerCase().replaceAll("[%_\\\\]", "");
        return PageResponse.of(users.search("%" + text + "%", PageRequest.of(Paging.page(page), Paging.size(size))),
                AdminUserItem::of);
    }

    /**
     * @throws ConflictException     if an administrator tries to block themselves
     * @throws UserNotFoundException if there is no such account
     */
    @Transactional
    public AdminUserItem setActive(Long adminId, Long userId, Boolean active) {
        if (active == null) {
            throw new BadRequestException("`active` must be true or false");
        }
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!active && user.getId().equals(adminId)) {
            throw new ConflictException("CANNOT_BLOCK_SELF", "You cannot block your own account");
        }
        user.setActive(active);
        if (!active) {
            sessions.revokeAll(userId);
        }
        return AdminUserItem.of(user);
    }
}
