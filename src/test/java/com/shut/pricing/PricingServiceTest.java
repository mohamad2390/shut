package com.shut.pricing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private static final Long USER = 1L;

    private final PriceItemRepository repository = mock(PriceItemRepository.class);
    private final PricingService service = new PricingService(repository);

    @Test
    void refusesToAddWhenTheMenuIsFull() {
        when(repository.countByUserId(USER)).thenReturn((long) PricingService.MAX_ITEMS);

        assertThatThrownBy(() -> service.add(USER, "عکاسی", PricingUnit.HOUR, 1_000_000))
                .isInstanceOf(PriceLimitReachedException.class);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
