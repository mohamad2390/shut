package com.shut.equipment;

import com.shut.common.text.PersianDigits;

import java.util.Locale;

/** How names are compared, so the same gear typed in different ways is one product. */
public final class EquipmentNames {

    private EquipmentNames() {
    }

    /**
     * The comparison key of a name: lower case, Persian letters and digits instead of Arabic and Latin-looking
     * variants, and nothing but letters and digits. "Alpha 7", "alpha-7" and "ALPHA۷" are all {@code alpha7}.
     *
     * @throws IllegalArgumentException if nothing is left, for example for "---"
     */
    public static String key(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is empty");
        }
        String key = PersianDigits.toAscii(name.replace('ي', 'ی').replace('ك', 'ک'))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Name has no letters or digits: " + name);
        }
        return key;
    }

    /** The name as it is shown: trimmed, with every run of spaces made one. */
    public static String display(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }
}
