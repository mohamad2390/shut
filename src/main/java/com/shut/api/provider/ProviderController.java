package com.shut.api.provider;

import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only view of the specialists. */
@RestController
@RequestMapping(ApiPaths.Providers.BASE)
public class ProviderController {

    private final ProviderApiService providers;

    public ProviderController(ProviderApiService providers) {
        this.providers = providers;
    }

    /** {@code q} is optional; every word of it must match. Pages count from 0, best rated first. */
    @GetMapping
    public PageResponse<ProviderListItem> list(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "0") int size) {
        return providers.list(q, Paging.page(page), Paging.size(size));
    }

    @GetMapping(ApiPaths.Providers.BY_ID)
    public ProviderDetailsResponse details(@PathVariable Long id) {
        return providers.details(id);
    }
}
