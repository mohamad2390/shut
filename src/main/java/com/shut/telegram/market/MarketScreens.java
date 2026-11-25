package com.shut.telegram.market;

import com.shut.common.text.PersianDigits;
import com.shut.market.Listing;
import com.shut.market.Offer;
import com.shut.market.OfferStatus;
import com.shut.telegram.menu.MenuCallbacks;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/** The pages anyone sees: the market section, the list, a listing and the member's own offers. */
final class MarketScreens {

    private MarketScreens() {
    }

    static Screen home() {
        return new Screen("🛒 بازار تجهیزات\n\nتجهیزات دست‌دوم و نو از خود اعضا. پرداخت و تحویل بین خریدار و فروشنده‌ست؛ "
                + "اینجا آگهی و پیشنهاد قیمت رد و بدل می‌شه. اگه چیزی برای فروش داری، آگهی بذار.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🔎 دیدن آگهی‌ها", MarketCallbacks.list(0)))
                        .row(InlineKeyboardButton.callback("📦 آگهی‌های من", MarketOwnerCallbacks.MINE))
                        .row(InlineKeyboardButton.callback("💬 پیشنهادهای من", MarketCallbacks.MY_OFFERS))
                        .row(InlineKeyboardButton.callback("🏠 منوی اصلی", MenuCallbacks.HOME))
                        .build());
    }

    static Screen list(Page<Listing> listings) {
        if (listings.isEmpty()) {
            return new Screen("هنوز آگهی‌ای نیست. اولین نفر باش!", new KeyboardBuilder()
                    .row(InlineKeyboardButton.callback("📦 ثبت آگهی من", MarketOwnerCallbacks.MINE))
                    .row(InlineKeyboardButton.callback("↩️ بازار تجهیزات", MarketCallbacks.HOME))
                    .build());
        }
        KeyboardBuilder keyboard = new KeyboardBuilder();
        listings.forEach(listing -> keyboard.row(InlineKeyboardButton.callback(MarketTexts.listLine(listing),
                MarketCallbacks.listing(listing.getId()))));
        if (listings.hasPrevious()) {
            keyboard.row(InlineKeyboardButton.callback("▶️ قبلی", MarketCallbacks.list(listings.getNumber() - 1)));
        }
        if (listings.hasNext()) {
            keyboard.row(InlineKeyboardButton.callback("بعدی ◀️", MarketCallbacks.list(listings.getNumber() + 1)));
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ بازار تجهیزات", MarketCallbacks.HOME));
        return new Screen("🛒 " + PersianDigits.toPersian(listings.getTotalElements())
                + " آگهی\n\nروی هر آگهی بزن تا جزئیاتش رو ببینی.", keyboard.build());
    }

    static Screen listing(Listing listing, String category, long photoCount, String sellerName) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (photoCount > 0) {
            keyboard.row(InlineKeyboardButton.callback("🖼 عکس‌ها", MarketCallbacks.photos(listing.getId())));
        }
        keyboard.row(InlineKeyboardButton.callback("💰 خرید / پیشنهاد قیمت", MarketCallbacks.offer(listing.getId())));
        keyboard.row(InlineKeyboardButton.callback("↩️ لیست آگهی‌ها", MarketCallbacks.list(0)));
        return new Screen(MarketTexts.details(listing, category, photoCount, sellerName), keyboard.build());
    }

    static Screen gone() {
        return new Screen("این آگهی دیگه در دسترس نیست.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("↩️ لیست آگهی‌ها", MarketCallbacks.list(0)))
                .build());
    }

    static Screen askAmount(Listing listing) {
        return new Screen("💰 پیشنهاد برای «" + listing.getTitle() + "»\n\nقیمت آگهی " + MarketTexts.money(listing.getPrice())
                + " است. مبلغ پیشنهادی‌ت رو به تومان بفرست، یا اگه با قیمت آگهی می‌خری دکمه‌ی پایین رو بزن."
                + "\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ با قیمت آگهی می‌خرم", MarketCallbacks.atAskingPrice(listing.getId())))
                        .row(InlineKeyboardButton.callback("انصراف", MarketCallbacks.listing(listing.getId())))
                        .build());
    }

    static Screen askNote(Listing listing) {
        return new Screen("یک پیام کوتاه برای فروشنده بفرست؛ مثلاً امکان دیدن حضوری یا نحوه‌ی تحویل. "
                + "اگه پیامی نداری دکمه‌ی پایین رو بزن.\n\nبرای انصراف /cancel رو بزن.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("بدون پیام", MarketCallbacks.skipNote(listing.getId())))
                        .row(InlineKeyboardButton.callback("انصراف", MarketCallbacks.listing(listing.getId())))
                        .build());
    }

    static Screen offerSent(Listing listing) {
        return new Screen("✅ پیشنهادت برای «" + listing.getTitle()
                + "» رفت. وقتی فروشنده جواب بده همین‌جا خبرت می‌کنم و راه ارتباطش رو می‌فرستم.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("💬 پیشنهادهای من", MarketCallbacks.MY_OFFERS))
                        .row(InlineKeyboardButton.callback("↩️ بازار تجهیزات", MarketCallbacks.HOME))
                        .build());
    }

    /** The member's latest offers; {@code listings} holds the listings they were for, by id. */
    static Screen myOffers(List<Offer> offers, Map<Long, Listing> listings) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (offers.isEmpty()) {
            text = "هنوز پیشنهادی ندادی. از «دیدن آگهی‌ها» یک آگهی انتخاب کن.";
        } else {
            StringBuilder lines = new StringBuilder("💬 آخرین پیشنهادهای تو\n");
            offers.forEach(offer -> {
                Listing listing = listings.get(offer.getListingId());
                lines.append("\n").append(listing == null ? "آگهی حذف‌شده" : listing.getTitle())
                        .append(" — ").append(MarketTexts.offerAmount(offer))
                        .append("\n   ").append(offer.getStatus().label());
                if (listing != null && offer.getStatus() == OfferStatus.ACCEPTED) {
                    keyboard.row(InlineKeyboardButton.callback("✅ " + listing.getTitle(), MarketCallbacks.listing(listing.getId())));
                }
            });
            text = lines.toString();
        }
        keyboard.row(InlineKeyboardButton.callback("↩️ بازار تجهیزات", MarketCallbacks.HOME));
        return new Screen(text, keyboard.build());
    }
}
