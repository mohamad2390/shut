package com.shut.telegram.market;

import com.shut.market.Listing;
import com.shut.market.MarketEvents;
import com.shut.market.Offer;
import com.shut.market.OfferStatus;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.messaging.TelegramLinks;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tells the seller about a new offer and the buyer about the answer, in Telegram. It is the bot's side of
 * {@link MarketEvents}, so offers made on the web reach members who use the bot too. A failed message is only logged.
 */
@Component
@TelegramBotEnabled
public class MarketNotifier implements MarketEvents {

    private static final Logger log = LoggerFactory.getLogger(MarketNotifier.class);

    private final UserService users;
    private final BotMessenger messenger;

    MarketNotifier(UserService users, BotMessenger messenger) {
        this.users = users;
        this.messenger = messenger;
    }

    /** The member's name for someone else to read; their Telegram username or, without one, their phone is added. */
    String nameOf(Long userId) {
        User user = users.findById(userId);
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (name.isEmpty()) {
            name = "یک عضو";
        }
        if (user.getTelegramUsername() != null) {
            return name + " (@" + user.getTelegramUsername() + ")";
        }
        return user.getPhoneNumber() == null ? name : name + " (" + user.getPhoneNumber() + ")";
    }

    @Override
    public void offerArrived(Listing listing, Offer offer) {
        MarketOwnerScreens.OfferLine line = new MarketOwnerScreens.OfferLine(offer, nameOf(offer.getBuyerId()));
        send(listing.getSellerId(), new Screen("💬 پیشنهاد تازه برای «" + listing.getTitle() + "»\n\n"
                + MarketOwnerScreens.offerText(line),
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ قبول", MarketOwnerCallbacks.accept(offer.getId())),
                                InlineKeyboardButton.callback("❌ رد", MarketOwnerCallbacks.decline(offer.getId())))
                        .row(InlineKeyboardButton.callback("📦 مدیریت آگهی", MarketOwnerCallbacks.manage(listing.getId())))
                        .build()));
    }

    @Override
    public void offerAnswered(Listing listing, Offer offer) {
        boolean accepted = offer.getStatus() == OfferStatus.ACCEPTED;
        String text = (accepted ? "✅ " : "❌ ") + "پیشنهادت برای «" + listing.getTitle() + "» "
                + (accepted ? "قبول شد." : "رد شد.");
        KeyboardBuilder keyboard = new KeyboardBuilder();
        if (accepted) {
            User seller = users.findById(listing.getSellerId());
            text += "\n\nراه ارتباط فروشنده: " + nameOf(listing.getSellerId())
                    + "\nبرای هماهنگی پرداخت و تحویل باهاش در تماس باش.";
            TelegramLinks.messageButton(seller.getTelegramUsername()).ifPresent(keyboard::row);
        }
        keyboard.row(InlineKeyboardButton.callback("💬 پیشنهادهای من", MarketCallbacks.MY_OFFERS));
        send(offer.getBuyerId(), new Screen(text, keyboard.build()));
    }

    private void send(Long userId, Screen screen) {
        try {
            Long telegramId = users.findById(userId).getTelegramId();
            if (telegramId == null) {
                return; // a web-only member: they see the offer or the answer on the web
            }
            messenger.send(telegramId, screen);
        } catch (RuntimeException e) {
            log.warn("Could not notify user {} about a market offer: {}", userId, e.getMessage());
        }
    }
}
