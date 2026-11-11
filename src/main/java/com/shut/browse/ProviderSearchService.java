package com.shut.browse;

import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.profile.entity.ProfessionalProfile;
import com.shut.profile.repository.ProfessionalProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Lets members find each other. Only active members who have set a display name are listed. */
@Service
public class ProviderSearchService {

    private final ProfessionalProfileRepository profiles;

    public ProviderSearchService(ProfessionalProfileRepository profiles) {
        this.profiles = profiles;
    }

    /**
     * Members matching the query by name, headline, city, skills or service cities, ordered by name.
     * A blank query lists everyone. The searching user is never part of the result.
     */
    @Transactional(readOnly = true)
    public Page<ProviderSummary> search(String query, Long searcherUserId, int page, int size) {
        return profiles.search(SearchPattern.of(query), searcherUserId, PageRequest.of(Math.max(0, page), size))
                .map(profile -> new ProviderSummary(profile.getUser().getId(), profile.getDisplayName(),
                        profile.getHeadline(), profile.getBaseCity()));
    }

    /** A member's public page, or empty if they do not exist, are inactive or have not set a display name. */
    @Transactional(readOnly = true)
    public Optional<ProviderDetails> find(Long userId) {
        return profiles.findByUser_Id(userId)
                .filter(profile -> profile.getUser().isActive() && profile.getDisplayName() != null)
                .map(ProviderSearchService::toDetails);
    }

    private static ProviderDetails toDetails(ProfessionalProfile profile) {
        return new ProviderDetails(ProfessionalProfileResponse.from(profile), profile.getUser().getTelegramUsername());
    }
}
