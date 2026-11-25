package com.shut.market;

public enum OfferStatus {
    PENDING("منتظر پاسخ"),
    ACCEPTED("قبول شد"),
    DECLINED("رد شد"),
    WITHDRAWN("پس گرفته شد");

    private final String label;

    OfferStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
