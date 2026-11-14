package com.shut.venue;

/** Where a booking request stands. */
public enum BookingStatus {
    PENDING("⏳ منتظر پاسخ"),
    ACCEPTED("✅ قبول شد"),
    DECLINED("❌ رد شد");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
