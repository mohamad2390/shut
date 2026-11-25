package com.shut.business;

import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Which business categories an account works in, and the cards of the studio and rental ones. */
@Service
public class BusinessService {

    private final BusinessProfileRepository profiles;
    private final RoleService roles;

    public BusinessService(BusinessProfileRepository profiles, RoleService roles) {
        this.profiles = profiles;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Optional<BusinessProfile> find(Long userId, BusinessKind kind) {
        return profiles.findByUserIdAndKind(userId, kind);
    }

    /** Creates the card or replaces the one the account already has, and gives the account the matching role. */
    @Transactional
    public BusinessProfile register(User user, BusinessKind kind, String name, String city, String description) {
        BusinessProfile profile = profiles.findByUserIdAndKind(user.getId(), kind)
                .orElseGet(() -> new BusinessProfile(user.getId(), kind, name, city, description));
        profile.change(name, city, description);
        roles.grant(user, kind.roleCode());
        return profiles.save(profile);
    }

    /** Gives the account the venue owner role; the venues themselves are managed in the venue section. */
    @Transactional
    public void becomeVenueOwner(User user) {
        roles.grant(user, RoleCodes.VENUE_OWNER);
    }

    /** The kinds with a card that the account already has. */
    @Transactional(readOnly = true)
    public Set<BusinessKind> kindsOf(Long userId) {
        Set<BusinessKind> kinds = EnumSet.noneOf(BusinessKind.class);
        profiles.findByUserId(userId).forEach(profile -> kinds.add(profile.getKind()));
        return kinds;
    }
}
