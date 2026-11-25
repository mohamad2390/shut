package com.shut.telegram.venue;

import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.user.service.UserService;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenueCalendarService;
import com.shut.venue.VenuePhoto;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles the buttons for browsing venues, reading their calendar and asking for a booking. */
@Component
@TelegramBotEnabled
class VenueCallbackHandler extends CallbackHandler {

    static final String VENUE_KEY = "venue";
    static final String DAY_KEY = "day";
    private static final int PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(VenueCallbackHandler.class);

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final UserService users;
    private final VenueService venues;
    private final VenueCalendarService venueCalendar;
    private final VenueBookingService bookings;
    private final CalendarService dates;

    VenueCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                         ConversationService conversations, TelegramAccountResolver accounts, UserService users,
                         VenueService venues, VenueCalendarService venueCalendar, VenueBookingService bookings,
                         CalendarService dates) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.users = users;
        this.venues = venues;
        this.venueCalendar = venueCalendar;
        this.bookings = bookings;
        this.dates = dates;
    }

    @Override
    protected String prefix() {
        return VenueCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> showFresh(context, VenueScreens.home());
            case "list" -> showList(context);
            case "v" -> openVenue(context);
            case "ph" -> sendPhotos(context);
            case "cal" -> openCalendar(context);
            case "book" -> startBooking(context);
            case "bt" -> chooseFullDay(context);
            case "mybook" -> showMyBookings(context);
            default -> { }
        }
    }

    private void showList(CallbackContext context) {
        Page<Venue> page = venues.listed(page(context.arg(1)), PAGE_SIZE);
        Map<Long, List<VenuePrice>> prices = venues.pricesOf(page.stream().map(Venue::getId).toList());
        show(context, VenueScreens.list(page, prices));
    }

    private void openVenue(CallbackContext context) {
        conversations.clear(context.user().id());
        show(context, listedVenue(context.arg(1)).map(this::venuePage).orElseGet(VenueScreens::gone));
    }

    private Screen venuePage(Venue venue) {
        return VenueScreens.venue(venue, venues.photoCount(venue.getId()), venues.prices(venue.getId()),
                users.findById(venue.getOwnerId()).getTelegramUsername());
    }

    /** The photos go out one by one, then the venue page again so the buttons are at the bottom. */
    private void sendPhotos(CallbackContext context) {
        Optional<Venue> venue = listedVenue(context.arg(1));
        if (venue.isEmpty()) {
            show(context, VenueScreens.gone());
            return;
        }
        List<VenuePhoto> photos = venues.photos(venue.get().getId());
        if (photos.isEmpty()) {
            messenger.sendText(context.chatId(), "این مکان هنوز عکسی نداره.");
            return;
        }
        photos.forEach(photo -> sendPhoto(context.chatId(), photo));
        messenger.send(context.chatId(), venuePage(venue.get()));
    }

    /** One broken file must not stop the rest from being sent. */
    private void sendPhoto(long chatId, VenuePhoto photo) {
        try {
            telegram.sendMedia(SendMediaRequest.photo(chatId, photo.getFileId(), null, null));
        } catch (TelegramApiException e) {
            log.warn("Could not send venue photo {}: {}", photo.getId(), e.getMessage());
        }
    }

    private void openCalendar(CallbackContext context) {
        Optional<Venue> venue = listedVenue(context.arg(1));
        if (venue.isEmpty()) {
            show(context, VenueScreens.gone());
            return;
        }
        JalaliMonth requested = VenueArgs.month(context.arg(2), context.arg(3)).orElse(JalaliMonth.of(dates.today()));
        show(context, monthScreen(venue.get(), requested));
    }

    private Screen monthScreen(Venue venue, JalaliMonth requested) {
        JalaliMonth month = dates.clamp(requested);
        long id = venue.getId();
        Map<LocalDate, CalendarStatus> statuses = venueCalendar.statuses(id, month.atDay(1), month.atDay(month.length()));
        LocalDate today = dates.today();
        return VenueCalendarScreen.month(venue.getName(), month, statuses,
                "روی یک روز آزاد (🟢) بزن و ساعت مورد نظرت رو بگو.",
                date -> statuses.get(date) == CalendarStatus.FREE && !date.isBefore(today)
                        ? VenueCallbacks.book(id, date) : null,
                target -> VenueCallbacks.calendar(id, target),
                dates.hasPreviousMonth(month), dates.hasNextMonth(month),
                InlineKeyboardButton.callback("↩️ صفحه‌ی مکان", VenueCallbacks.venue(id)));
    }

    /** A free day was tapped: ask which hours are wanted. */
    private void startBooking(CallbackContext context) {
        Optional<Venue> venue = bookableVenue(context);
        Optional<LocalDate> day = VenueArgs.date(context.arg(2));
        if (venue.isEmpty() || day.isEmpty()) {
            return;
        }
        if (!isOpenOn(venue.get(), day.get())) {
            show(context, monthScreen(venue.get(), JalaliMonth.of(day.get())));
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_BOOKING_TIME, bookingData(venue.get(), day.get()));
        show(context, VenueScreens.askTime(venue.get(), day.get()));
    }

    /** "The whole day" was tapped: no hours, go on to the note. */
    private void chooseFullDay(CallbackContext context) {
        Optional<Venue> venue = bookableVenue(context);
        Optional<LocalDate> day = VenueArgs.date(context.arg(2));
        if (venue.isEmpty() || day.isEmpty()) {
            return;
        }
        if (!isOpenOn(venue.get(), day.get())) {
            show(context, monthScreen(venue.get(), JalaliMonth.of(day.get())));
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_BOOKING_NOTE, bookingData(venue.get(), day.get()));
        show(context, VenueScreens.askNote(venue.get(), "تمام روز", day.get()));
    }

    /** The listed venue in the callback, if it is not the user's own; otherwise the user is told and nothing happens. */
    private Optional<Venue> bookableVenue(CallbackContext context) {
        Optional<Venue> venue = listedVenue(context.arg(1));
        if (venue.isEmpty()) {
            show(context, VenueScreens.gone());
            return venue;
        }
        if (venue.get().getOwnerId().equals(accounts.resolve(context.user()).getId())) {
            messenger.sendText(context.chatId(), "این مکان خودته؛ نمی‌تونی برای خودت درخواست بدی.");
            return Optional.empty();
        }
        return venue;
    }

    private boolean isOpenOn(Venue venue, LocalDate day) {
        return !day.isBefore(dates.today()) && venueCalendar.isFree(venue.getId(), day);
    }

    private static Map<String, String> bookingData(Venue venue, LocalDate day) {
        Map<String, String> data = new HashMap<>();
        data.put(VENUE_KEY, String.valueOf(venue.getId()));
        data.put(DAY_KEY, day.toString());
        return data;
    }

    private void showMyBookings(CallbackContext context) {
        List<VenueBooking> latest = bookings.latestOf(accounts.resolve(context.user()).getId());
        Map<Long, Venue> byId = new HashMap<>();
        latest.forEach(booking -> venues.find(booking.getVenueId()).ifPresent(venue -> byId.put(venue.getId(), venue)));
        show(context, VenueScreens.myBookings(latest, byId));
    }

    private Optional<Venue> listedVenue(String rawId) {
        return VenueArgs.id(rawId).flatMap(venues::findListed);
    }

    private static int page(String raw) {
        return VenueArgs.id(raw).map(Long::intValue).filter(page -> page >= 0).orElse(0);
    }

    private void showFresh(CallbackContext context, Screen screen) {
        conversations.clear(context.user().id());
        show(context, screen);
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
