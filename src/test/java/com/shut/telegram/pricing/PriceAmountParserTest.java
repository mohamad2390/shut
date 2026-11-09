package com.shut.telegram.pricing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceAmountParserTest {

    @Test
    void readsPlainAndPersianDigits() {
        assertThat(PriceAmountParser.parse("500000")).isEqualTo(500_000L);
        assertThat(PriceAmountParser.parse("۱۰۰۰۰۰۰")).isEqualTo(1_000_000L);
    }

    @Test
    void ignoresThousandsSeparatorsAndSpaces() {
        assertThat(PriceAmountParser.parse("1,500,000")).isEqualTo(1_500_000L);
        assertThat(PriceAmountParser.parse("۱٬۵۰۰٬۰۰۰")).isEqualTo(1_500_000L);
        assertThat(PriceAmountParser.parse(" 2 000 000 ")).isEqualTo(2_000_000L);
    }

    @Test
    void rejectsZeroNegativeTextAndHugeAmounts() {
        assertThatThrownBy(() -> PriceAmountParser.parse("0")).isInstanceOf(InvalidPriceInputException.class);
        assertThatThrownBy(() -> PriceAmountParser.parse("-5")).isInstanceOf(InvalidPriceInputException.class);
        assertThatThrownBy(() -> PriceAmountParser.parse("پانصد هزار")).isInstanceOf(InvalidPriceInputException.class);
        assertThatThrownBy(() -> PriceAmountParser.parse("10000000001")).isInstanceOf(InvalidPriceInputException.class);
        assertThatThrownBy(() -> PriceAmountParser.parse("12.5")).isInstanceOf(InvalidPriceInputException.class);
    }
}
