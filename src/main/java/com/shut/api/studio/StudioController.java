package com.shut.api.studio;

import com.shut.api.PageResponse;
import com.shut.api.Paging;
import com.shut.common.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only view of the studios. */
@RestController
@RequestMapping(ApiPaths.Studios.BASE)
public class StudioController {

    private final StudioApiService studios;

    public StudioController(StudioApiService studios) {
        this.studios = studios;
    }

    @GetMapping
    public PageResponse<StudioListItem> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "0") int size) {
        return studios.list(Paging.page(page), Paging.size(size));
    }

    @GetMapping(ApiPaths.Studios.BY_ID)
    public StudioDetailsResponse details(@PathVariable Long id) {
        return studios.details(id);
    }
}
