package com.shut.api.me;

import com.shut.api.showcase.ShowcaseCategoryResponse;
import com.shut.api.venue.LabelResponse;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.messaging.BotLinks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** A read-only view of the member's own showcase. Adding to it is done in the bot only. */
@Service
public class MyShowcaseApiService {

    private final ShowcaseService showcase;
    private final ObjectProvider<BotLinks> botLinks;

    public MyShowcaseApiService(ShowcaseService showcase, ObjectProvider<BotLinks> botLinks) {
        this.showcase = showcase;
        this.botLinks = botLinks;
    }

    public MyShowcaseResponse get(Long userId) {
        Map<ShowcaseCategory, Long> counts = showcase.countsByCategory(userId);
        List<ShowcaseCategoryResponse> categories = Arrays.stream(ShowcaseCategory.values())
                .filter(category -> counts.getOrDefault(category, 0L) > 0)
                .map(category -> new ShowcaseCategoryResponse(
                        new LabelResponse(category.name(), category.label()), counts.get(category)))
                .toList();
        long total = categories.stream().mapToLong(ShowcaseCategoryResponse::count).sum();
        BotLinks links = botLinks.getIfAvailable();
        return new MyShowcaseResponse(categories, total, links == null ? null : links.botUrl().orElse(null));
    }
}
