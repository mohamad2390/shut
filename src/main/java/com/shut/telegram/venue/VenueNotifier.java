package com.shut.telegram.venue;

import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import com.shut.venue.BookingStatus;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Tells the owner about a new booking request and the member about the answer. A failed message is only logged. */
@Component
@TelegramBotEnabled
public class VenueNotifier {

    private static final Logger log = LoggerFactory.getLogger(VenueNotifier.class);

    private final UserService users;
    private final BotMessenger messenger;

    VenueNotifier(UserService users, BotMessenger messenger) {
        this.users = users;
        this.messenger = messenger;
    }

    /** The member's name for the owner to read; their Telegram username is added when they have one. */
    public String nameOf(Long userId) {
        User user = users.findById(userId);
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (name.isEmpty()) {
            name = "یک عضو";
        }
        if (user.getTelegramUsername() != null) {
            return name + " (@" + user.getTelegramUsername() + ")";
        }
        // Someone who signed up on the web has no Telegram username; their phone is how the owner reaches them.
        return user.getPhoneNumber() == null ? name : name + " (" + user.getPhoneNumber() + ")";
    }

    /** Sends the owner the request with buttons to accept or decline it. */
    public void requestArrived(Venue venue, VenueBooking booking) {
        RequestLine line = new RequestLine(booking, nameOf(booking.getRequesterId()));
        send(venue.getOwnerId(), new Screen("📨 درخواست رزرو تازه برای " + venue.getName() + "\n\n"
                + VenueOwnerScreens.requestText(line),
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ قبول", VenueOwnerCallbacks.accept(booking.getId())),
                                InlineKeyboardButton.callback("❌ رد", VenueOwnerCallbacks.decline(booking.getId())))
                        .build()));
    }

    /** Tells the member whether the owner accepted. */
    public void answered(Venue venue, VenueBooking booking) {
        boolean accepted = booking.getStatus() == BookingStatus.ACCEPTED;
        String text = (accepted ? "✅ " : "❌ ") + "درخواستت برای " + venue.getName() + " در "
                + VenueTexts.date(booking.getDay()) + "، " + VenueTexts.hours(booking) + (accepted ? " قبول شد." : " رد شد.");
        send(booking.getRequesterId(), new Screen(text + (accepted ? "\nبرای هماهنگی با صاحب مکان در تماس باش." : ""),
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("📍 صفحه‌ی مکان", VenueCallbacks.venue(venue.getId())))
                        .build()));
    }

    private void send(Long userId, Screen screen) {
        try {
            Long telegramId = users.findById(userId).getTelegramId();
            if (telegramId == null) {
                return; // a web-only member: they see the request or the answer on the web
            }
            messenger.send(telegramId, screen);
        } catch (RuntimeException e) {
            log.warn("Could not notify user {} about a venue booking: {}", userId, e.getMessage());
        }
    }
}
