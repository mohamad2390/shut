package com.shut.telegram.venue;

import com.shut.calendar.CalendarService;
import com.shut.calendar.CalendarStatus;
import com.shut.common.calendar.JalaliMonth;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.storage.MediaBackupService;
import com.shut.venue.Venue;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenueCalendarService;
import com.shut.venue.VenuePhoto;
import com.shut.venue.VenuePriceUnit;
import com.shut.venue.VenueService;
import com.shut.venue.VenueType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Handles the owner's buttons: their venues, the details of each, photos, calendar and booking requests. */
@Component
@TelegramBotEnabled
class VenueOwnerCallbackHandler extends CallbackHandler {

    static final String FIELD_KEY = "field";
    static final String UNIT_KEY = "unit";
    static final String AMOUNT_KEY = "amount";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final VenueService venues;
    private final VenueCalendarService venueCalendar;
    private final VenueBookingService bookings;
    private final CalendarService dates;
    private final VenueNotifier notifier;
    private final MediaBackupService backups;

    VenueOwnerCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                              ConversationService conversations, TelegramAccountResolver accounts,
                              VenueService venues, VenueCalendarService venueCalendar, VenueBookingService bookings,
                              CalendarService dates, VenueNotifier notifier, MediaBackupService backups) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.venues = venues;
        this.venueCalendar = venueCalendar;
        this.bookings = bookings;
        this.dates = dates;
        this.notifier = notifier;
        this.backups = backups;
    }

    @Override
    protected String prefix() {
        return VenueOwnerCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "mine" -> showMine(context);
            case "new" -> startNew(context);
            case "m" -> withVenue(context, venue -> manage(context, venue));
            case "e" -> withVenue(context, venue -> startEditing(context, venue));
            case "type" -> withVenue(context, VenueOwnerScreens::pickType);
            case "st" -> withVenue(context, venue -> setType(context, venue));
            case "pr" -> withVenue(context, venue -> pricesScreen(context, venue));
            case "pa" -> withVenue(context, VenueOwnerScreens::pickPriceUnit);
            case "pu" -> withVenue(context, venue -> startPrice(context, venue));
            case "px" -> removePrice(context);
            case "ph" -> withVenue(context, venue -> startPhotos(context, venue));
            case "phx" -> withVenue(context, venue -> clearPhotos(context, venue));
            case "oc" -> withVenue(context, venue -> calendarScreen(context, venue,
                    VenueArgs.month(context.arg(2), context.arg(3)).orElse(JalaliMonth.of(dates.today()))));
            case "od" -> withVenue(context, venue -> markDay(context, venue));
            case "rq" -> withVenue(context, this::requestsScreen);
            case "ba" -> respond(context, true);
            case "bd" -> respond(context, false);
            case "tg" -> withVenue(context, venue -> toggle(context, venue));
            case "del" -> withVenue(context, VenueOwnerScreens::confirmDelete);
            case "delok" -> deleteVenue(context);
            default -> { }
        }
    }

    private void showMine(CallbackContext context) {
        conversations.clear(context.user().id());
        List<Venue> mine = venues.ownedBy(userId(context));
        Map<Long, Long> pending = new HashMap<>();
        mine.forEach(venue -> pending.put(venue.getId(), bookings.pendingCount(venue.getId())));
        show(context, VenueOwnerScreens.mine(mine, pending));
    }

    private void startNew(CallbackContext context) {
        if (venues.ownedBy(userId(context)).size() >= VenueService.MAX_VENUES_PER_OWNER) {
            show(context, VenueOwnerScreens.limitReached());
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_VENUE_NAME, Map.of());
        show(context, VenueOwnerScreens.askName());
    }

    private Screen manage(CallbackContext context, Venue venue) {
        conversations.clear(context.user().id());
        return manageScreen(venue);
    }

    private Screen manageScreen(Venue venue) {
        return VenueOwnerScreens.manage(venue, venues.photoCount(venue.getId()), bookings.pendingCount(venue.getId()),
                venues.prices(venue.getId()));
    }

    private Screen startEditing(CallbackContext context, Venue venue) {
        Optional<VenueField> field = VenueField.fromKey(context.arg(2));
        if (field.isEmpty()) {
            return manageScreen(venue);
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_VENUE_FIELD,
                Map.of(VenueCallbackHandler.VENUE_KEY, String.valueOf(venue.getId()), FIELD_KEY, field.get().name()));
        return new Screen(field.get().prompt(), new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("انصراف", VenueOwnerCallbacks.manage(venue.getId())))
                .build());
    }

    private Screen setType(CallbackContext context, Venue venue) {
        VenueType.fromKey(context.arg(2)).ifPresent(type -> venues.update(venue.getId(), venue.getOwnerId(), v -> v.setType(type)));
        return manageScreen(reload(venue));
    }

    private Screen pricesScreen(CallbackContext context, Venue venue) {
        conversations.clear(context.user().id());
        return VenueOwnerScreens.prices(venue, venues.prices(venue.getId()));
    }

    private Screen startPrice(CallbackContext context, Venue venue) {
        Optional<VenuePriceUnit> unit = VenuePriceUnit.fromKey(context.arg(2));
        if (unit.isEmpty()) {
            return VenueOwnerScreens.pickPriceUnit(venue);
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_VENUE_PRICE_AMOUNT,
                Map.of(VenueCallbackHandler.VENUE_KEY, String.valueOf(venue.getId()), UNIT_KEY, unit.get().name()));
        return VenueOwnerScreens.askPriceAmount(venue, unit.get());
    }

    private void removePrice(CallbackContext context) {
        Optional<Long> venueId = VenueArgs.id(context.arg(1)).flatMap(id -> venues.removePrice(id, userId(context)));
        show(context, venueId.flatMap(id -> venues.findOwned(id, userId(context)))
                .map(venue -> VenueOwnerScreens.prices(venue, venues.prices(venue.getId())))
                .orElseGet(VenueOwnerScreens::gone));
    }

    private Screen startPhotos(CallbackContext context, Venue venue) {
        conversations.begin(context.user().id(), ConversationStep.AWAITING_VENUE_PHOTO,
                Map.of(VenueCallbackHandler.VENUE_KEY, String.valueOf(venue.getId())));
        return VenueOwnerScreens.addPhotos(venue, venues.photoCount(venue.getId()));
    }

    private Screen clearPhotos(CallbackContext context, Venue venue) {
        venues.clearPhotos(venue.getId(), venue.getOwnerId()).stream()
                .map(VenuePhoto::getBackupMessageId)
                .forEach(backups::remove);
        return manageScreen(venue);
    }

    private Screen calendarScreen(CallbackContext context, Venue venue, JalaliMonth requested) {
        JalaliMonth month = dates.clamp(requested);
        long id = venue.getId();
        Map<LocalDate, CalendarStatus> statuses = venueCalendar.statuses(id, month.atDay(1), month.atDay(month.length()));
        LocalDate today = dates.today();
        LocalDate last = dates.lastMarkableDay();
        return VenueCalendarScreen.month(venue.getName(), month, statuses,
                "روی هر روز بزن تا وضعیتش عوض بشه: آزاد، پر، و دوباره نامشخص. «آزاد» یعنی قابل رزرو و محدودیتی روی تعداد درخواست‌ها نیست. «پر» یعنی دیگه درخواست نمی‌گیری؛ قبول کردن رزرو روز رو خودکار پر نمی‌کنه.",
                date -> !date.isBefore(today) && !date.isAfter(last) ? VenueOwnerCallbacks.day(id, date) : null,
                target -> VenueOwnerCallbacks.calendar(id, target),
                dates.hasPreviousMonth(month), dates.hasNextMonth(month),
                InlineKeyboardButton.callback("↩️ مدیریت مکان", VenueOwnerCallbacks.manage(id)));
    }

    private Screen markDay(CallbackContext context, Venue venue) {
        Optional<LocalDate> day = VenueArgs.date(context.arg(2));
        if (day.isEmpty()) {
            return manageScreen(venue);
        }
        try {
            venueCalendar.cycle(venue.getId(), venue.getOwnerId(), day.get());
        } catch (IllegalArgumentException ignored) {
            // the day is outside the range that can be marked; just redraw the month
        }
        return calendarScreen(context, venue, JalaliMonth.of(day.get()));
    }

    private Screen requestsScreen(Venue venue) {
        List<RequestLine> lines = bookings.pending(venue.getId(), venue.getOwnerId()).stream()
                .map(booking -> new RequestLine(booking, notifier.nameOf(booking.getRequesterId())))
                .toList();
        return VenueOwnerScreens.requests(venue, lines);
    }

    /** Accepts or declines a request, tells the member, and shows the owner what is still waiting. */
    private void respond(CallbackContext context, boolean accept) {
        Long ownerId = userId(context);
        Optional<VenueBooking> answered = VenueArgs.id(context.arg(1))
                .flatMap(id -> bookings.respond(id, ownerId, accept));
        if (answered.isEmpty()) {
            messenger.sendText(context.chatId(), "این درخواست دیگه منتظر جواب نیست.");
            return;
        }
        Optional<Venue> venue = venues.findOwned(answered.get().getVenueId(), ownerId);
        venue.ifPresent(found -> notifier.answered(found, answered.get()));
        show(context, venue.map(this::requestsScreen).orElseGet(VenueOwnerScreens::gone));
    }

    private Screen toggle(CallbackContext context, Venue venue) {
        venues.update(venue.getId(), venue.getOwnerId(), v -> v.setActive(!v.isActive()));
        return manageScreen(reload(venue));
    }

    private void deleteVenue(CallbackContext context) {
        Long ownerId = userId(context);
        VenueArgs.id(context.arg(1)).flatMap(id -> venues.delete(id, ownerId))
                .ifPresent(photos -> photos.stream().map(VenuePhoto::getBackupMessageId).forEach(backups::remove));
        showMine(context);
    }

    /** The venue as it is now, after a change made through the service. */
    private Venue reload(Venue venue) {
        return venues.find(venue.getId()).orElse(venue);
    }

    /** Runs the action on the venue in the callback if it is the user's, and shows what it returns. */
    private void withVenue(CallbackContext context, Function<Venue, Screen> action) {
        Screen screen = VenueArgs.id(context.arg(1))
                .flatMap(id -> venues.findOwned(id, userId(context)))
                .map(action)
                .orElseGet(VenueOwnerScreens::gone);
        show(context, screen);
    }

    private Long userId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
