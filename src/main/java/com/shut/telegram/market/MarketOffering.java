package com.shut.telegram.market;

import com.shut.market.Listing;
import com.shut.market.MarketException;
import com.shut.market.MarketService;
import com.shut.market.Offer;
import com.shut.market.OfferService;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.messaging.BotMessenger;

import java.util.Map;
import java.util.Optional;

/** Sends an offer once the buyer has given the amount (or the asking price) and the note, and says what happened. */
final class MarketOffering {

    private MarketOffering() {
    }

    /**
     * @param data the conversation data: the listing id and, unless the offer is at the asking price, the amount
     * @param note the message for the seller, or null for none
     */
    static void send(ConversationService conversations, OfferService offers, MarketService market,
                     MarketNotifier notifier, BotMessenger messenger, long telegramId, long chatId, Long buyerId,
                     Map<String, String> data, String note) {
        conversations.clear(telegramId);
        Optional<Listing> listing = parse(data.get(MarketCallbackHandler.LISTING_KEY)).flatMap(market::find);
        if (listing.isEmpty()) {
            messenger.send(chatId, MarketScreens.gone());
            return;
        }
        Long amount = parse(data.get(MarketCallbackHandler.AMOUNT_KEY)).orElse(null);
        Offer offer;
        try {
            offer = offers.make(listing.get().getId(), buyerId, amount, note);
        } catch (MarketException e) {
            messenger.sendText(chatId, refusal(e.reason()));
            messenger.send(chatId, MarketScreens.home());
            return;
        }
        notifier.offerArrived(listing.get(), offer);
        messenger.send(chatId, MarketScreens.offerSent(listing.get()));
    }

    private static String refusal(MarketException.Reason reason) {
        return switch (reason) {
            case LISTING_NOT_AVAILABLE -> "این آگهی دیگه در دسترس نیست.";
            case OWN_LISTING -> "این آگهی خودته؛ نمی‌تونی برای خودت پیشنهاد بدی.";
            case ALREADY_OFFERED -> "پیشنهاد قبلیت برای این آگهی هنوز منتظر جواب فروشنده‌ست.";
            case INVALID_AMOUNT -> "مبلغ پیشنهاد معتبر نیست.";
        };
    }

    private static Optional<Long> parse(String raw) {
        try {
            return raw == null ? Optional.empty() : Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
