package com.shut.browse;

import com.shut.profile.repository.ProfessionalProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lists every member, best rated first. Members nobody has rated yet come last, in name order. */
@Service
public class ProviderRankingService {

    static final int MAX_SKILLS = 3;

    private final ProfessionalProfileRepository profiles;

    public ProviderRankingService(ProfessionalProfileRepository profiles) {
        this.profiles = profiles;
    }

    /** One page of everyone; {@code page} counts from 0. */
    public Page<RankedProvider> page(int page, int size) {
        return page(null, page, size);
    }

    /**
     * One page of the ranking of the members that match {@code query} (everyone if it is blank), counted from 0.
     * The words are matched the way the search does. Each member carries up to three of their skills.
     */
    @Transactional(readOnly = true)
    public Page<RankedProvider> page(String query, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(0, page), size);
        List<String> words = SearchPattern.words(query);
        Page<ProfessionalProfileRepository.RankingRow> rows;
        if (words.isEmpty()) {
            rows = profiles.ranking(request);
        } else {
            List<Long> matching = profiles.findAll(ProviderSearchSpecification.matching(words, 0L)).stream()
                    .map(profile -> profile.getUser().getId()).toList();
            rows = matching.isEmpty() ? Page.empty(request) : profiles.rankingAmong(matching, request);
        }
        Map<Long, List<String>> skills = skillsOf(rows.map(ProfessionalProfileRepository.RankingRow::getUserId).getContent());
        return rows.map(row -> new RankedProvider(row.getUserId(), row.getDisplayName(), row.getCity(),
                row.getAverage() == null ? 0 : row.getAverage(), row.getTotal(),
                skills.getOrDefault(row.getUserId(), List.of())));
    }

    private Map<Long, List<String>> skillsOf(List<Long> userIds) {
        Map<Long, List<String>> skills = new HashMap<>();
        if (userIds.isEmpty()) {
            return skills;
        }
        for (ProfessionalProfileRepository.SkillRow row : profiles.skillsOf(userIds)) {
            List<String> own = skills.computeIfAbsent(row.getUserId(), id -> new ArrayList<>());
            if (own.size() < MAX_SKILLS) {
                own.add(row.getSkill());
            }
        }
        return skills;
    }
}
