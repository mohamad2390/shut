package com.shut.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingCatalogTest {

    private final ServiceTypeRepository services = mock(ServiceTypeRepository.class);
    private final UnitIconRepository unitIcons = mock(UnitIconRepository.class);
    private final PricingCatalog catalog = new PricingCatalog(services, unitIcons);

    private static ServiceType service(long id, String name, String icon, boolean active) {
        ServiceType service = new ServiceType(name, icon);
        ReflectionTestUtils.setField(service, "id", id);
        ReflectionTestUtils.setField(service, "active", active);
        return service;
    }

    @Test
    void listsTheServicesThatAreOnOffer() {
        List<ServiceType> active = List.of(service(1, "عکاسی پرتره", "📷", true));
        when(services.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(active);

        assertThat(catalog.services()).isEqualTo(active);
    }

    @Test
    void aServiceThatIsNoLongerOfferedCannotBePicked() {
        when(services.findById(2L)).thenReturn(Optional.of(service(2, "قدیمی", "🧩", false)));
        when(services.findById(1L)).thenReturn(Optional.of(service(1, "عکاسی پرتره", "📷", true)));
        when(services.findById(3L)).thenReturn(Optional.empty());

        assertThat(catalog.service(1)).isPresent();
        assertThat(catalog.service(2)).isEmpty();
        assertThat(catalog.service(3)).isEmpty();
    }

    @Test
    void everyUnitHasAnIconAndTheTableOverridesTheDefault() {
        when(unitIcons.findAll()).thenReturn(List.of(new UnitIcon(PricingUnit.HOUR, "🕐")));

        Map<PricingUnit, String> icons = catalog.unitIcons();

        assertThat(icons).containsOnlyKeys(PricingUnit.values());
        assertThat(icons.get(PricingUnit.HOUR)).isEqualTo("🕐");
        assertThat(icons.get(PricingUnit.DAY)).isEqualTo(PricingCatalog.DEFAULT_UNIT_ICON);
    }

    @Test
    void aRowForAUnitThatDoesNotExistIsIgnored() {
        UnitIcon stale = new UnitIcon(PricingUnit.HOUR, "🕐");
        ReflectionTestUtils.setField(stale, "unit", "FORTNIGHT");
        when(unitIcons.findAll()).thenReturn(List.of(stale));

        assertThat(catalog.unitIcons().values()).containsOnly(PricingCatalog.DEFAULT_UNIT_ICON);
    }
}
