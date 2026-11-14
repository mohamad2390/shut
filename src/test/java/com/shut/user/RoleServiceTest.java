package com.shut.user;

import com.shut.user.entity.Role;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.repository.RoleRepository;
import com.shut.user.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {

    private final RoleRepository roles = mock(RoleRepository.class);
    private final RoleService service = new RoleService(roles);

    private static Role role(String code, String title, boolean isDefault) {
        Role role = new Role(code, title);
        ReflectionTestUtils.setField(role, "defaultRole", isDefault);
        return role;
    }

    private static User user() {
        return new User(1L, "علی");
    }

    @Test
    void aNewUserHasNoRoleUntilItIsGivenOne() {
        assertThat(user().getRoles()).isEmpty();
        assertThat(user().isProvider()).isFalse();
    }

    @Test
    void grantsTheRoleWithTheGivenCode() {
        when(roles.findByCode(RoleCodes.PROVIDER)).thenReturn(Optional.of(role(RoleCodes.PROVIDER, "متخصص", false)));
        User user = user();

        service.grant(user, RoleCodes.PROVIDER);

        assertThat(user.hasRole(RoleCodes.PROVIDER)).isTrue();
        assertThat(user.isProvider()).isTrue();
        assertThat(user.hasRole(RoleCodes.ADMIN)).isFalse();
    }

    @Test
    void grantingTheSameRoleTwiceKeepsOne() {
        when(roles.findByCode(RoleCodes.PROVIDER)).thenReturn(Optional.of(role(RoleCodes.PROVIDER, "متخصص", false)));
        User user = user();

        service.grant(user, RoleCodes.PROVIDER);
        service.grant(user, RoleCodes.PROVIDER);

        assertThat(user.getRoles()).hasSize(1);
    }

    @Test
    void aRoleAddedLaterInTheDatabaseCanBeGivenWithoutNewCode() {
        when(roles.findByCode("EDITOR")).thenReturn(Optional.of(role("EDITOR", "ادیتور", false)));
        User user = user();

        service.grant(user, "EDITOR");

        assertThat(user.hasRole("EDITOR")).isTrue();
    }

    @Test
    void refusesARoleThatDoesNotExist() {
        when(roles.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grant(user(), "NOPE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void takesARoleAwayButNeverADefaultOne() {
        Role customer = role(RoleCodes.CUSTOMER, "کاربر عادی", true);
        Role provider = role(RoleCodes.PROVIDER, "متخصص", false);
        when(roles.findByCode(RoleCodes.CUSTOMER)).thenReturn(Optional.of(customer));
        when(roles.findByCode(RoleCodes.PROVIDER)).thenReturn(Optional.of(provider));
        User user = user();
        user.addRole(customer);
        user.addRole(provider);

        service.revoke(user, RoleCodes.PROVIDER);
        service.revoke(user, RoleCodes.CUSTOMER);

        assertThat(user.hasRole(RoleCodes.PROVIDER)).isFalse();
        assertThat(user.hasRole(RoleCodes.CUSTOMER)).isTrue();
    }

    @Test
    void twoRolesWithTheSameCodeAreTheSameRole() {
        assertThat(new Role("PROVIDER", "متخصص")).isEqualTo(new Role("PROVIDER", "متخصص ۲"));
    }
}
