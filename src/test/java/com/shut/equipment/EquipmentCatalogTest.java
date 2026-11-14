package com.shut.equipment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentCatalogTest {

    @Mock EquipmentTypeRepository types;
    @Mock EquipmentBrandRepository brands;
    @Mock EquipmentBrandAliasRepository aliases;
    @Mock EquipmentModelRepository models;

    EquipmentCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new EquipmentCatalog(types, brands, aliases, models);
    }

    @Test
    void unknownBrandIsAddedAndUnverified() {
        when(brands.findByNormalizedName("zhiyun")).thenReturn(Optional.empty());
        when(aliases.findByNormalizedAlias("zhiyun")).thenReturn(Optional.empty());
        when(brands.save(any(EquipmentBrand.class))).thenAnswer(call -> call.getArgument(0));

        Resolved<EquipmentBrand> result = catalog.brandNamed("  Zhiyun ", 5L);

        assertThat(result.created()).isTrue();
        assertThat(result.value().getName()).isEqualTo("Zhiyun");
        assertThat(result.value().isVerified()).isFalse();
        assertThat(result.value().getCreatedByUserId()).isEqualTo(5L);
    }

    @Test
    void knownBrandIsFoundNotAdded() {
        EquipmentBrand sony = new EquipmentBrand("سونی", null);
        when(brands.findByNormalizedName("سونی")).thenReturn(Optional.of(sony));

        Resolved<EquipmentBrand> result = catalog.brandNamed("سونی", 5L);

        assertThat(result.created()).isFalse();
        assertThat(result.value()).isSameAs(sony);
        verify(brands, never()).save(any());
    }

    @Test
    void brandIsFoundByAnotherSpelling() {
        EquipmentBrand sony = new EquipmentBrand("سونی", null);
        EquipmentBrandAlias alias = org.mockito.Mockito.mock(EquipmentBrandAlias.class);
        when(alias.getBrandId()).thenReturn(1L);
        when(brands.findByNormalizedName("sony")).thenReturn(Optional.empty());
        when(aliases.findByNormalizedAlias("sony")).thenReturn(Optional.of(alias));
        when(brands.findById(1L)).thenReturn(Optional.of(sony));

        Resolved<EquipmentBrand> result = catalog.brandNamed("Sony", 5L);

        assertThat(result.created()).isFalse();
        assertThat(result.value()).isSameAs(sony);
        verify(brands, never()).save(any());
    }

    @Test
    void existingModelOfSameTypeAndBrandIsReused() {
        EquipmentType type = new EquipmentType("دوربین عکاسی", null);
        EquipmentBrand brand = new EquipmentBrand("سونی", null);
        EquipmentModel existing = new EquipmentModel(type, brand, "Alpha 7", null);
        when(types.findById(1L)).thenReturn(Optional.of(type));
        when(brands.findById(2L)).thenReturn(Optional.of(brand));
        when(models.findByType_IdAndBrand_IdAndNormalizedName(1L, 2L, "alpha7")).thenReturn(Optional.of(existing));

        Resolved<EquipmentModel> result = catalog.modelNamed(1L, 2L, "alpha-7", 5L);

        assertThat(result.created()).isFalse();
        assertThat(result.value()).isSameAs(existing);
        verify(models, never()).save(any());
    }

    @Test
    void newModelBecomesAProductOfTypeBrandAndName() {
        EquipmentType type = new EquipmentType("دوربین عکاسی", null);
        EquipmentBrand brand = new EquipmentBrand("سونی", null);
        when(types.findById(1L)).thenReturn(Optional.of(type));
        when(brands.findById(2L)).thenReturn(Optional.of(brand));
        when(models.findByType_IdAndBrand_IdAndNormalizedName(1L, 2L, "a7iv")).thenReturn(Optional.empty());
        when(models.save(any(EquipmentModel.class))).thenAnswer(call -> call.getArgument(0));

        Resolved<EquipmentModel> result = catalog.modelNamed(1L, 2L, "A7 IV", 5L);

        assertThat(result.created()).isTrue();
        assertThat(result.value().displayName()).isEqualTo("سونی A7 IV");
        assertThat(result.value().getType()).isSameAs(type);
    }
}
