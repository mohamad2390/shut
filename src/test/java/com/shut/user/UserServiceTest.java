package com.shut.user;

import com.shut.user.dto.UserOnboardingRequest;
import com.shut.user.entity.Role;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.repository.UserRepository;
import com.shut.user.service.RoleService;
import com.shut.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleService roles = mock(RoleService.class);
    private final UserService service = new UserService(users, roles);

    private static UserOnboardingRequest request() {
        return new UserOnboardingRequest(10L, "ali", "علی", "رضایی");
    }

    @Test
    void aNewAccountGetsTheDefaultRoles() {
        when(users.findByTelegramId(10L)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(roles.defaultRoles()).thenReturn(List.of(new Role(RoleCodes.CUSTOMER, "کاربر عادی")));

        User user = service.onboard(request());

        assertThat(user.hasRole(RoleCodes.CUSTOMER)).isTrue();
        assertThat(user.isProvider()).isFalse();
    }

    @Test
    void anExistingAccountKeepsItsRolesAndGetsNoneAgain() {
        User existing = new User(10L, "علی");
        existing.addRole(new Role(RoleCodes.PROVIDER, "متخصص"));
        when(users.findByTelegramId(10L)).thenReturn(Optional.of(existing));

        User user = service.onboard(request());

        assertThat(user.isProvider()).isTrue();
        verify(roles, never()).defaultRoles();
    }
}
