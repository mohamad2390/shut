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

        assertThatThrownBy(() -> service.add(USER, new ServiceType("عکاسی پرتره", "📷"), PricingUnit.HOUR, 1_000_000))
                .isInstanceOf(PriceLimitReachedException.class);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void addsAPriceFromAServiceAndTakesItsNameAndIcon() {
        when(repository.countByUserId(USER)).thenReturn(0L);
        when(repository.save(org.mockito.ArgumentMatchers.any(PriceItem.class))).thenAnswer(call -> call.getArgument(0));

        PriceItem item = service.add(USER, new ServiceType("عکاسی عروسی", "💍"), PricingUnit.DAY, 5_000_000);

        org.assertj.core.api.Assertions.assertThat(item.getTitle()).isEqualTo("عکاسی عروسی");
        org.assertj.core.api.Assertions.assertThat(item.getIcon()).isEqualTo("💍");
        org.assertj.core.api.Assertions.assertThat(item.getUnit()).isEqualTo(PricingUnit.DAY);
    }

    @Test
    void aNameChosenByTheMemberReplacesTheNameOfTheServiceButKeepsItsIcon() {
        when(repository.countByUserId(USER)).thenReturn(0L);
        when(repository.save(org.mockito.ArgumentMatchers.any(PriceItem.class))).thenAnswer(call -> call.getArgument(0));

        PriceItem item = service.add(USER, new ServiceType("سایر خدمات", "🧩"), "  عکاسی از غذا ", PricingUnit.HOUR, 700_000);

        org.assertj.core.api.Assertions.assertThat(item.getTitle()).isEqualTo("عکاسی از غذا");
        org.assertj.core.api.Assertions.assertThat(item.getIcon()).isEqualTo("🧩");
    }

    @Test
    void aBlankNameMeansTheNameOfTheService() {
        ServiceType other = new ServiceType("سایر خدمات", "🧩");

        org.assertj.core.api.Assertions.assertThat(new PriceItem(USER, other, "  ", PricingUnit.HOUR, 1).getTitle())
                .isEqualTo("سایر خدمات");
        org.assertj.core.api.Assertions.assertThat(new PriceItem(USER, other, null, PricingUnit.HOUR, 1).getTitle())
                .isEqualTo("سایر خدمات");
    }

    @Test
    void aPriceTypedByHandShowsTheDefaultIcon() {
        PriceItem item = new PriceItem(USER, "قدیمی", PricingUnit.HOUR, 1);

        org.assertj.core.api.Assertions.assertThat(item.getIcon()).isEqualTo(PricingCatalog.DEFAULT_SERVICE_ICON);
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
