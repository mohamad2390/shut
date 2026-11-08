package com.shut.profile.exception;

public class ProfessionalProfileNotFoundException extends RuntimeException {
    public ProfessionalProfileNotFoundException(Long userId) {
        super("پروفایل حرفه‌ای برای کاربر با شناسه " + userId + " پیدا نشد");
    }
}
