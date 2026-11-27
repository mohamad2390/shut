package com.shut.telegram.equipment;

/**
 * Callback data for the equipment pages. The type, brand and model pickers are shared by every feature that lets a
 * member choose gear from the reference list; the {@code purpose} in the data says which feature asked, see
 * {@link EquipmentChoice}.
 */
public final class EquipmentCallbacks {

    public static final String PREFIX = "equip";
    /** The member's own gear. The profile page's equipment button leads here. */
    public static final String HOME = PREFIX + ":home";

    private EquipmentCallbacks() {
    }

    /** Opens the list of types for the feature with this purpose; the picker starts here. */
    public static String choose(String purpose) {
        return PREFIX + ":add:" + purpose;
    }

    static String otherType(String purpose) {
        return PREFIX + ":ot:" + purpose;
    }

    static String type(String purpose, long typeId) {
        return PREFIX + ":t:" + purpose + ":" + typeId;
    }

    static String brand(String purpose, long typeId, long brandId) {
        return PREFIX + ":b:" + purpose + ":" + typeId + ":" + brandId;
    }

    static String model(String purpose, long modelId) {
        return PREFIX + ":m:" + purpose + ":" + modelId;
    }

    static String otherBrand(String purpose, long typeId) {
        return PREFIX + ":ob:" + purpose + ":" + typeId;
    }

    static String otherModel(String purpose, long typeId, long brandId) {
        return PREFIX + ":om:" + purpose + ":" + typeId + ":" + brandId;
    }

    static String remove(long userEquipmentId) {
        return PREFIX + ":rm:" + userEquipmentId;
    }
}
