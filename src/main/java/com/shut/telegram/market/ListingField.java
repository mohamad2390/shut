package com.shut.telegram.market;

import com.shut.common.text.PersianDigits;
import com.shut.market.Listing;
import com.shut.pricing.PricingService;
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;

import java.util.Optional;

/** The details of a listing a seller can type, with how each is checked. Optional ones are cleared by sending "-". */
enum ListingField {
    TITLE("عنوان", "عنوان آگهی رو بفرست؛ مثلاً «سونی A7 III بدنه».", 120, true),
    PRICE("قیمت", "قیمت رو به تومان بفرست؛ مثلاً ۵۰٬۰۰۰٬۰۰۰.", 0, true),
    BRAND("برند", "برند رو بفرست؛ مثلاً Sony. برای پاک کردن «-» بفرست.", 60, false),
    MODEL("مدل", "مدل رو بفرست؛ مثلاً A7 III. برای پاک کردن «-» بفرست.", 80, false),
    CITY("شهر", "شهر رو بفرست تا خریدارها بتونن شهرت رو ببینن. برای پاک کردن «-» بفرست.", 100, false),
    DESCRIPTION("توضیحات", "توضیحات رو بفرست: تعداد شات، ایرادها، لوازم همراه، دلیل فروش… برای پاک کردن «-» بفرست.", 2000, false);

    private final String label;
    private final String prompt;
    private final int maxLength;
    private final boolean required;

    ListingField(String label, String prompt, int maxLength, boolean required) {
        this.label = label;
        this.prompt = prompt;
        this.maxLength = maxLength;
        this.required = required;
    }

    String label() {
        return label;
    }

    String prompt() {
        return prompt;
    }

    static Optional<ListingField> fromKey(String key) {
        try {
            return Optional.of(valueOf(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /** @throws InvalidListingInputException if the text is not valid; the listing is then left alone */
    void applyTo(Listing listing, String raw) {
        if (this == PRICE) {
            listing.setPrice(price(raw));
            return;
        }
        String value = clean(raw);
        if (!required && (value.isEmpty() || value.equals("-"))) {
            value = null;
        }
        if (value == null) {
            clear(listing);
            return;
        }
        if (value.isEmpty() || value.length() > maxLength) {
            throw new InvalidListingInputException(label + " باید بین ۱ تا " + PersianDigits.toPersian(maxLength) + " حرف باشه.");
        }
        switch (this) {
            case TITLE -> listing.setTitle(value);
            case BRAND -> listing.setBrand(value);
            case MODEL -> listing.setModel(value);
            case CITY -> listing.setCity(value);
            case DESCRIPTION -> listing.setDescription(value);
            default -> throw new IllegalStateException();
        }
    }

    /** The price the seller typed. */
    static long price(String raw) {
        try {
            long amount = PriceAmountParser.parse(raw);
            if (amount <= 0 || amount > PricingService.MAX_AMOUNT) {
                throw new InvalidListingInputException("قیمت باید بیشتر از صفر باشه.");
            }
            return amount;
        } catch (InvalidPriceInputException e) {
            throw new InvalidListingInputException(e.getMessage());
        }
    }

    /** A required text (the title) typed while creating a listing. */
    static String title(String raw) {
        String value = clean(raw);
        if (value.isEmpty() || value.length() > TITLE.maxLength) {
            throw new InvalidListingInputException("عنوان باید بین ۱ تا ۱۲۰ حرف باشه.");
        }
        return value;
    }

    private void clear(Listing listing) {
        switch (this) {
            case BRAND -> listing.setBrand(null);
            case MODEL -> listing.setModel(null);
            case CITY -> listing.setCity(null);
            case DESCRIPTION -> listing.setDescription(null);
            default -> throw new IllegalStateException();
        }
    }

    private static String clean(String raw) {
        return raw.strip().replaceAll("\\s+", " ");
    }
}
