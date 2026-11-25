package com.shut.auth;

import com.shut.user.entity.User;
import com.shut.user.repository.UserRepository;
import com.shut.user.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The account behind a phone number. If the phone already belongs to an account (for instance one the bot knows), the
 * web sign-in opens that same account, so bot and web show the same person. Otherwise a web-only account is created.
 */
@Service
public class WebAccountService {

    private final UserRepository users;
    private final RoleService roles;

    public WebAccountService(UserRepository users, RoleService roles) {
        this.users = users;
        this.roles = roles;
    }

    /**
     * @param phone normalized, see {@link PhoneNumbers}
     * @throws UnauthorizedException if the account is disabled
     */
    @Transactional
    public User accountFor(String phone) {
        User user = users.findByPhoneNumber(phone).orElseGet(() -> create(phone));
        if (!user.isActive()) {
            throw new UnauthorizedException("This account is disabled");
        }
        return user;
    }

    private User create(String phone) {
        User user = new User(null, null);
        user.setPhoneNumber(phone);
        roles.defaultRoles().forEach(user::addRole);
        return users.save(user);
    }
}
