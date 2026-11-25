package com.shut.auth;

import com.shut.common.text.PersianDigits;

/** Iranian mobile numbers, always stored as {@code 09xxxxxxxxx}. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * Accepts Persian or Latin digits and the usual prefixes: {@code ۰۹۱۲…}, {@code 912…}, {@code +98912…},
     * {@code 0098912…}; spaces, dashes and brackets are ignored.
     *
     * @throws InvalidPhoneException if it is not a mobile number
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new InvalidPhoneException();
        }
        String digits = PersianDigits.toAscii(raw).replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+98")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("0098")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("98") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        } else if (digits.length() == 10 && digits.startsWith("9")) {
            digits = "0" + digits;
        }
        if (!digits.matches("09\\d{9}")) {
            throw new InvalidPhoneException();
        }
        return digits;
    }
}
