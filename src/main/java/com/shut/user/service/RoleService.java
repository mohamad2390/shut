package com.shut.user.service;

import com.shut.user.entity.Role;
import com.shut.user.entity.User;
import com.shut.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Looks roles up and gives them to accounts. */
@Service
public class RoleService {

    private final RoleRepository roles;

    public RoleService(RoleRepository roles) {
        this.roles = roles;
    }

    /** The roles every new account gets. */
    @Transactional(readOnly = true)
    public List<Role> defaultRoles() {
        return roles.findByDefaultRoleTrueOrderBySortOrderAscIdAsc();
    }

    /** Gives the account the role if it does not have it yet. Throws if no role has that code. */
    @Transactional
    public void grant(User user, String code) {
        user.addRole(roles.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code)));
    }

    /** Takes the role away from the account. Default roles stay, so every account is at least an ordinary user. */
    @Transactional
    public void revoke(User user, String code) {
        roles.findByCode(code).ifPresent(user::removeRole);
    }
}
