package com.shut.api.provider;

import com.shut.api.PageResponse;
import com.shut.browse.ProviderDetails;
import com.shut.browse.ProviderNotFoundException;
import com.shut.browse.ProviderRankingService;
import com.shut.browse.ProviderSearchService;
import com.shut.equipment.EquipmentService;
import com.shut.profile.dto.ProfessionalProfileResponse;
import com.shut.review.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The specialists for the web app. It calls the same services the bot uses (ranking, search, reviews, equipment), so a
 * search or a rating gives the same answer in both.
 */
@Service
public class ProviderApiService {

    private final ProviderRankingService ranking;
    private final ProviderSearchService search;
    private final ReviewService reviews;
    private final EquipmentService equipment;

    public ProviderApiService(ProviderRankingService ranking, ProviderSearchService search, ReviewService reviews,
                              EquipmentService equipment) {
        this.ranking = ranking;
        this.search = search;
        this.reviews = reviews;
        this.equipment = equipment;
    }

    /** Specialists matching the query (all if blank), best rated first. */
    public PageResponse<ProviderListItem> list(String query, int page, int size) {
        return PageResponse.of(ranking.page(query, page, size), provider -> new ProviderListItem(
                provider.userId(), provider.displayName(), provider.city(),
                new RatingResponse(provider.average(), provider.ratings()), provider.skills()));
    }

    /** @throws ProviderNotFoundException if the member does not exist, is inactive or has no display name */
    public ProviderDetailsResponse details(Long userId) {
        ProviderDetails details = search.find(userId).orElseThrow(() -> new ProviderNotFoundException(userId));
        ProfessionalProfileResponse profile = details.profile();
        List<EquipmentItemResponse> gear = equipment.list(userId).stream()
                .map(item -> new EquipmentItemResponse(item.getModel().getType().getName(),
                        item.getModel().getBrand().getName(), item.getModel().getName()))
                .toList();
        return new ProviderDetailsResponse(profile.userId(), profile.displayName(), profile.headline(), profile.bio(),
                profile.yearsExperience(), profile.baseCity(), profile.baseCountry(), profile.websiteUrl(),
                profile.instagramUsername(), details.telegramUsername(), profile.equipmentDescription(), gear,
                profile.skills(), profile.serviceCities(), profile.collaborators(),
                RatingResponse.of(reviews.summary(userId)));
    }
}
