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

    @Test
    void changesThePriceOfAnItemThatBelongsToTheUser() {
        PriceItem item = new PriceItem(USER, "عکاسی", PricingUnit.HOUR, 1_000_000);
        when(repository.findByIdAndUserId(5L, USER)).thenReturn(java.util.Optional.of(item));

        service.updateAmount(5L, USER, 1_500_000);

        org.assertj.core.api.Assertions.assertThat(item.getAmount()).isEqualTo(1_500_000);
    }

    @Test
    void changesTheUnitOfAnItem() {
        PriceItem item = new PriceItem(USER, "ادیت", PricingUnit.HOUR, 500_000);
        when(repository.findByIdAndUserId(5L, USER)).thenReturn(java.util.Optional.of(item));

        service.updateUnit(5L, USER, PricingUnit.PHOTO);

        org.assertj.core.api.Assertions.assertThat(item.getUnit()).isEqualTo(PricingUnit.PHOTO);
    }

    @Test
    void doesNotTouchAnItemThatIsNotTheUsers() {
        when(repository.findByIdAndUserId(5L, USER)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThat(service.updateAmount(5L, USER, 1)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(service.updateUnit(5L, USER, PricingUnit.DAY)).isEmpty();
    }
}
