package com.shut.api.showcase;

import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only view of a specialist's portfolio. */
@RestController
@RequestMapping(ApiPaths.Providers.BASE)
public class ShowcaseController {

    private final ShowcaseApiService showcase;

    public ShowcaseController(ShowcaseApiService showcase) {
        this.showcase = showcase;
    }

    @GetMapping(ApiPaths.Providers.SHOWCASE_CATEGORIES)
    public List<ShowcaseCategoryResponse> categories(@PathVariable Long id) {
        return showcase.categories(id);
    }

    /** {@code category} is required, e.g. {@code PHOTOGRAPHY}. */
    @GetMapping(ApiPaths.Providers.SHOWCASE)
    public PageResponse<ShowcaseItemResponse> items(@PathVariable Long id, @RequestParam String category,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "0") int size) {
        return showcase.items(id, category, Paging.page(page), Paging.size(size));
    }
}
