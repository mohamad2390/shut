package com.shut.telegram.showcase;

import com.shut.common.text.PersianDigits;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Everything the user sees in the showcase section. */
final class ShowcaseScreens {

    private ShowcaseScreens() {
    }

    /** The list of categories, each with how many items it holds. */
    static Screen home(Map<ShowcaseCategory, Long> counts) {
        List<InlineKeyboardButton> categoryButtons = Arrays.stream(ShowcaseCategory.values())
                .map(category -> {
                    long count = counts.getOrDefault(category, 0L);
                    String label = count > 0 ? category.label() + " (" + PersianDigits.toPersian(count) + ")" : category.label();
                    return InlineKeyboardButton.callback(label, ShowcaseCallbacks.category(category));
                })
                .toList();
        return new Screen(
                "🖼 ویترین\n\nنمونه‌کارهات رو توی دسته‌های جدا نگه دار. یه دسته رو انتخاب کن.",
                new KeyboardBuilder()
                        .grid(categoryButtons, 2)
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen category(ShowcaseCategory category, long count) {
        KeyboardBuilder keyboard = new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("➕ افزودن نمونه‌کار", ShowcaseCallbacks.add(category)));
        if (count > 0) {
            keyboard.row(InlineKeyboardButton.callback("📂 دیدن نمونه‌کارها", ShowcaseCallbacks.view(category, 0)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ دسته‌ها", ShowcaseCallbacks.HOME));
        return new Screen(
                "🖼 " + category.label() + "\n\n" + PersianDigits.toPersian(count) + " از "
                        + PersianDigits.toPersian(ShowcaseService.MAX_ITEMS_PER_CATEGORY) + " نمونه‌کار",
                keyboard.build());
    }

    static Screen uploadPrompt(ShowcaseCategory category, long remaining, boolean backupEnabled) {
        StringBuilder text = new StringBuilder("➕ " + category.label() + "\n\n")
                .append("عکس یا ویدیوهات رو بفرست. می‌تونی چندتا رو با هم بفرستی.\n")
                .append("برای حفظ کیفیت اصلی، عکس رو به‌صورت «فایل» (File) بفرست.\n")
                .append("اگه زیر فایل توضیح بنویسی، به‌عنوان توضیح نمونه‌کار ذخیره می‌شه.\n\n")
                .append("ظرفیت باقی‌مونده: ").append(PersianDigits.toPersian(remaining)).append(" نمونه‌کار\n");
        if (backupEnabled) {
            text.append("\nاز فایل‌هایی که اضافه می‌کنی یه نسخه‌ی پشتیبان توی آرشیو خصوصی ربات نگه داشته می‌شه "
                    + "و با حذف نمونه‌کار، اون نسخه هم پاک می‌شه.\n");
        }
        text.append("\nوقتی تموم شد، «تمام» رو بزن.");
        return new Screen(text.toString(), finishKeyboard(category));
    }

    static InlineKeyboardMarkup finishKeyboard(ShowcaseCategory category) {
        return new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("✅ تمام", ShowcaseCallbacks.done(category)))
                .build();
    }

    /** The "page 1 to 5 of 12" message under a batch of items, with next and back buttons. */
    static Screen pageNavigation(ShowcaseCategory category, int page, int pageSize, long total) {
        long first = (long) page * pageSize + 1;
        long last = Math.min(total, (long) (page + 1) * pageSize);
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (last < total) {
            keyboard.row(InlineKeyboardButton.callback("▶️ بعدی", ShowcaseCallbacks.view(category, page + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ برگشت", ShowcaseCallbacks.category(category)));
        return new Screen(
                category.label() + ": " + PersianDigits.toPersian(first) + " تا " + PersianDigits.toPersian(last)
                        + " از " + PersianDigits.toPersian(total),
                keyboard.build());
    }

    static InlineKeyboardMarkup deleteKeyboard(long itemId) {
        return new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("🗑 حذف", ShowcaseCallbacks.delete(itemId)))
                .build();
    }

    static InlineKeyboardMarkup confirmDeleteKeyboard(long itemId) {
        return new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("بله، حذف کن", ShowcaseCallbacks.confirmDelete(itemId)),
                        InlineKeyboardButton.callback("نه", ShowcaseCallbacks.keep(itemId)))
                .build();
    }
}
