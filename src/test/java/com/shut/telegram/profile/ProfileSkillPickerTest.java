package com.shut.telegram.profile;

import com.shut.pricing.PricingCatalog;
import com.shut.pricing.ServiceType;
import com.shut.profile.dto.ProfessionalProfileRequest;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.profile.service.ProfessionalProfileService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import com.shut.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileSkillPickerTest {

    private static final long USER = 7L;

    private final TelegramAccountResolver accounts = mock(TelegramAccountResolver.class);
    private final ProfessionalProfileService profiles = mock(ProfessionalProfileService.class);
    private final PricingCatalog catalog = mock(PricingCatalog.class);
    private final ProfileSkillPicker picker = new ProfileSkillPicker(accounts, profiles, catalog);
    private final TelegramUser from = mock(TelegramUser.class);

    private ServiceType photo = service(1, "عکاسی", false);
    private ServiceType other = service(9, "سایر خدمات", true);

    ProfileSkillPickerTest() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER);
        when(accounts.resolve(from)).thenReturn(user);
        when(catalog.services()).thenReturn(List.of(photo, other));
        when(catalog.service(1L)).thenReturn(Optional.of(photo));
        when(catalog.service(9L)).thenReturn(Optional.of(other));
    }

    private static ServiceType service(long id, String name, boolean asksName) {
        ServiceType service = mock(ServiceType.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        when(service.asksName()).thenReturn(asksName);
        return service;
    }

    private void currentSkills(Set<String> skills) {
        when(profiles.findByUserId(USER)).thenReturn(Optional.of(new ProfessionalProfileResponse(
                1L, USER, "علی", null, null, 0, null, null, null, null, null, skills, Set.of(), Set.of(), null, null)));
    }

    private Set<String> savedSkills() {
        ArgumentCaptor<ProfessionalProfileRequest> saved = ArgumentCaptor.forClass(ProfessionalProfileRequest.class);
        verify(profiles).saveOrUpdate(any(), saved.capture());
        return saved.getValue().skills();
    }

    @Test
    void theScreenOffersTheServicesButNotOtherServices() {
        currentSkills(Set.of());

        Screen screen = picker.screen(from);

        assertThat(screen.markup().inlineKeyboard().stream().flatMap(List::stream).map(b -> b.text()))
                .contains("🏷 عکاسی").doesNotContain("🏷 سایر خدمات");
    }

    @Test
    void tappingAServiceAddsItToTheSkillsAndKeepsTheTypedOnes() {
        currentSkills(Set.of("رتوش حرفه‌ای"));

        Screen screen = picker.toggle(from, 1L);

        assertThat(savedSkills()).containsExactlyInAnyOrder("رتوش حرفه‌ای", "عکاسی");
        assertThat(screen).isNotNull();
    }

    @Test
    void tappingAServiceTheMemberHasTakesItOut() {
        currentSkills(Set.of("عکاسی", "تدوین"));

        picker.toggle(from, 1L);

        assertThat(savedSkills()).containsExactly("تدوین");
    }

    @Test
    void theLimitStopsMoreSkillsAndSaysSo() {
        currentSkills(IntStream.range(0, ProfileSkillScreens.MAX_SKILLS).mapToObj(i -> "مهارت" + i)
                .collect(Collectors.toSet()));

        Screen screen = picker.toggle(from, 1L);

        verify(profiles, never()).saveOrUpdate(any(), any());
        assertThat(screen.text()).contains(ProfileSkillPicker.LIMIT_NOTE);
    }

    @Test
    void anUnknownOrOtherServiceChangesNothing() {
        currentSkills(Set.of("تدوین"));
        when(catalog.service(404L)).thenReturn(Optional.empty());

        picker.toggle(from, 404L);
        picker.toggle(from, 9L);

        verify(profiles, never()).saveOrUpdate(any(), any());
    }
}
