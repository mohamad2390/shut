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
class EquipmentServiceTest {

    @Mock UserEquipmentRepository owned;
    @Mock EquipmentModelRepository models;
    @Mock EquipmentModel model;

    EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(owned, models);
    }

    @Test
    void addsAProductOnce() {
        when(models.findById(9L)).thenReturn(Optional.of(model));
        when(owned.existsByUserIdAndModel_Id(1L, 9L)).thenReturn(false);
        when(owned.countByUserId(1L)).thenReturn(0L);

        assertThat(service.add(1L, 9L)).isEqualTo(EquipmentService.AddResult.ADDED);
        verify(owned).save(any(UserEquipment.class));
    }

    @Test
    void secondAddOfTheSameProductIsIgnored() {
        when(models.findById(9L)).thenReturn(Optional.of(model));
        when(owned.existsByUserIdAndModel_Id(1L, 9L)).thenReturn(true);

        assertThat(service.add(1L, 9L)).isEqualTo(EquipmentService.AddResult.ALREADY_OWNED);
        verify(owned, never()).save(any());
    }

    @Test
    void stopsAtTheLimit() {
        when(models.findById(9L)).thenReturn(Optional.of(model));
        when(owned.existsByUserIdAndModel_Id(1L, 9L)).thenReturn(false);
        when(owned.countByUserId(1L)).thenReturn((long) EquipmentService.MAX_ITEMS);

        assertThat(service.add(1L, 9L)).isEqualTo(EquipmentService.AddResult.LIMIT_REACHED);
        verify(owned, never()).save(any());
    }

    @Test
    void removesOnlyTheMembersOwnItem() {
        when(owned.findByIdAndUserId(4L, 1L)).thenReturn(Optional.empty());

        assertThat(service.remove(1L, 4L)).isFalse();
        verify(owned, never()).delete(any());
    }
}
