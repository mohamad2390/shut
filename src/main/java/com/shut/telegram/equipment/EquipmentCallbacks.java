package com.shut.telegram.equipment;

/** Callback data for the equipment pages. */
public final class EquipmentCallbacks {

    public static final String PREFIX = "equip";
    /** The member's own gear. The profile page's equipment button leads here. */
    public static final String HOME = PREFIX + ":home";
    static final String ADD = PREFIX + ":add";
    static final String OTHER_TYPE = PREFIX + ":ot";

    private EquipmentCallbacks() {
    }

    static String type(long typeId) {
        return PREFIX + ":t:" + typeId;
    }

    static String brand(long typeId, long brandId) {
        return PREFIX + ":b:" + typeId + ":" + brandId;
    }

    static String model(long modelId) {
        return PREFIX + ":m:" + modelId;
    }

    static String otherBrand(long typeId) {
        return PREFIX + ":ob:" + typeId;
    }

    static String otherModel(long typeId, long brandId) {
        return PREFIX + ":om:" + typeId + ":" + brandId;
    }

    static String remove(long userEquipmentId) {
        return PREFIX + ":rm:" + userEquipmentId;
    }
}
