package com.shut.telegram.pricing;

import com.shut.common.text.PersianDigits;
import com.shut.pricing.PriceItem;

/** A price item as one line of text, shared by the owner's price menu and the pages other members see. */
public final class PriceText {

    private PriceText() {
    }

    public static String describe(PriceItem item) {
        return item.getTitle() + ": " + PersianDigits.toPersianGrouped(item.getAmount())
                + " تومان، " + item.getUnit().phrase();
    }
}
