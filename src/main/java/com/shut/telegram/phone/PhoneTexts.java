package com.shut.telegram.phone;

/** What the bot says while linking a phone number. */
final class PhoneTexts {

    static final String ASK = "📱 برای ورود به سایت با همین حساب، شماره‌ی موبایلت رو بفرست.\n\n"
            + "دکمه‌ی «ارسال شماره‌ی من» زیر کادر نوشتن رو بزن. شماره فقط برای ورود به سایت استفاده می‌شه.\n"
            + "برای انصراف /cancel رو بفرست.";
    static final String BUTTON = "📱 ارسال شماره‌ی من";
    static final String USE_THE_BUTTON = "شماره رو تایپ نکن؛ دکمه‌ی «ارسال شماره‌ی من» رو بزن. برای انصراف /cancel رو بفرست.";
    static final String NOT_YOURS = "این شماره مال حساب تلگرام خودت نیست. فقط با دکمه‌ی «ارسال شماره‌ی من» شماره‌ی خودت رو بفرست.";
    static final String NOT_IRANIAN = "فقط شماره‌ی موبایل ایرانی (مثل ۰۹۱۲۱۲۳۴۵۶۷) قبول می‌شه.";
    static final String LINKED = "✅ شماره‌ات ثبت شد. حالا با همین شماره توی سایت وارد می‌شی و همین حساب رو می‌بینی.";
    static final String MERGED = "✅ شماره‌ات ثبت شد و ورودهای قبلی‌ات توی سایت به همین حساب وصل شد.";
    static final String ALREADY = "این شماره از قبل برای حساب تو ثبت شده؛ لازم نیست کاری بکنی.";
    static final String TAKEN = "این شماره برای یک حساب تلگرام دیگه ثبت شده. اگه شماره‌ی خودته، به پشتیبانی بگو.";

    private PhoneTexts() {
    }
}
