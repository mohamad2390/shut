package com.shut.telegram.handler;

/** Texts shared by several features. Feature-specific texts live with their feature. */
public final class BotMessages {

    public static final String JOIN_REQUIRED =
            "برای استفاده از ربات باید اول عضو کانال بشی. بعد از عضویت دوباره /start رو بزن.";
    public static final String JOIN_BUTTON = "عضویت در کانال";
    public static final String TEMPORARY_ERROR = "مشکلی پیش اومد. لطفاً چند لحظه دیگه دوباره امتحان کن.";
    public static final String MENU_INTRO = "منوی اصلی";
    public static final String CANCELLED = "انجام کار لغو شد.";

    private static final String FALLBACK_NAME = "دوست عزیز";

    private BotMessages() {
    }

    public static String welcome(String firstName) {
        String name = firstName == null || firstName.isBlank() ? FALLBACK_NAME : firstName;
        return "سلام " + name + " 👋\nبه شات خوش اومدی.";
    }
}
