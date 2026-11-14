package com.shut.equipment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentNamesTest {

    @Test
    void spellingVariantsShareOneKey() {
        assertThat(EquipmentNames.key("Alpha 7")).isEqualTo("alpha7")
                .isEqualTo(EquipmentNames.key("alpha-7"))
                .isEqualTo(EquipmentNames.key("ALPHA ۷"));
    }

    @Test
    void arabicLettersAreMadePersian() {
        assertThat(EquipmentNames.key("كانون")).isEqualTo(EquipmentNames.key("کانون"));
    }

    @Test
    void nameWithoutLettersOrDigitsIsRejected() {
        assertThatThrownBy(() -> EquipmentNames.key("---")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void displayNameIsTrimmedAndSingleSpaced() {
        assertThat(EquipmentNames.display("  Alpha   7 ")).isEqualTo("Alpha 7");
    }
}
