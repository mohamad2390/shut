package com.shut.api.showcase;

import com.shut.api.BadRequestException;
import com.shut.api.PageResponse;
import com.shut.api.venue.LabelResponse;
import com.shut.browse.ProviderNotFoundException;
import com.shut.browse.ProviderSearchService;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The portfolio of a specialist for the web app, from the same service the bot uses. */
@Service
public class ShowcaseApiService {

    private final ShowcaseService showcase;
    private final ProviderSearchService providers;

    public ShowcaseApiService(ShowcaseService showcase, ProviderSearchService providers) {
        this.showcase = showcase;
        this.providers = providers;
    }

    /** The sections that have items, in the bot's order. */
    public List<ShowcaseCategoryResponse> categories(Long userId) {
        visible(userId);
        Map<ShowcaseCategory, Long> counts = showcase.countsByCategory(userId);
        return Arrays.stream(ShowcaseCategory.values())
                .filter(category -> counts.getOrDefault(category, 0L) > 0)
                .map(category -> new ShowcaseCategoryResponse(
                        new LabelResponse(category.name(), category.label()), counts.get(category)))
                .toList();
    }

    /** Newest first.
     *
     * @throws BadRequestException if {@code category} is not one of the sections */
    public PageResponse<ShowcaseItemResponse> items(Long userId, String category, int page, int size) {
        visible(userId);
        ShowcaseCategory section = ShowcaseCategory.fromKey(category)
                .orElseThrow(() -> new BadRequestException("Unknown category: " + category));
        return PageResponse.of(showcase.page(userId, section, page, size), item -> new ShowcaseItemResponse(
                item.getId(), item.getMediaType().name(), item.getCaption()));
    }

    private void visible(Long userId) {
        providers.find(userId).orElseThrow(() -> new ProviderNotFoundException(userId));
    }
}
