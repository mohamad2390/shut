package com.shut.browse;

import com.shut.profile.repository.ProfessionalProfileRepository;
import com.shut.profile.repository.ProfessionalProfileRepository.RankingRow;
import com.shut.profile.repository.ProfessionalProfileRepository.SkillRow;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderRankingServiceTest {

    private final ProfessionalProfileRepository profiles = mock(ProfessionalProfileRepository.class);
    private final ProviderRankingService service = new ProviderRankingService(profiles);

    private static RankingRow row(long id, String name, String city, Double average, long total) {
        RankingRow row = mock(RankingRow.class);
        when(row.getUserId()).thenReturn(id);
        when(row.getDisplayName()).thenReturn(name);
        when(row.getCity()).thenReturn(city);
        when(row.getAverage()).thenReturn(average);
        when(row.getTotal()).thenReturn(total);
        return row;
    }

    private static SkillRow skill(long userId, String skill) {
        SkillRow row = mock(SkillRow.class);
        when(row.getUserId()).thenReturn(userId);
        when(row.getSkill()).thenReturn(skill);
        return row;
    }

    @Test
    void eachMemberGetsAtMostThreeOfTheirOwnSkills() {
        when(profiles.ranking(any())).thenReturn(new PageImpl<>(
                List.of(row(1, "علی", "یزد", 4.5, 2), row(2, "سارا", null, null, 0)), PageRequest.of(0, 5), 2));
        when(profiles.skillsOf(any())).thenReturn(List.of(skill(1, "الف"), skill(2, "ب"), skill(1, "پ"),
                skill(1, "ت"), skill(1, "ث")));

        Page<RankedProvider> page = service.page(0, 5);

        assertThat(page.getContent().get(0).skills()).containsExactly("الف", "پ", "ت");
        assertThat(page.getContent().get(1).skills()).containsExactly("ب");
        assertThat(page.getContent().get(1).average()).isZero();
    }

    @Test
    void anEmptyPageDoesNotAskForSkills() {
        when(profiles.ranking(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 5), 7));

        assertThat(service.page(3, 5).getContent()).isEmpty();

        verify(profiles, never()).skillsOf(any());
    }
}
