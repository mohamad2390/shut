package com.shut.telegram.venue;

import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import com.shut.venue.Venue;
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;
import com.shut.venue.VenueBooking;
import com.shut.venue.VenueBookingException;
import com.shut.venue.VenueBookingService;
import com.shut.venue.VenuePrice;
import com.shut.venue.VenuePriceUnit;
import com.shut.venue.VenueService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receives the text typed for a venue: its name, one of its details, or the note of a booking request. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class VenueInputHandler implements UpdateHandler {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_NOTE_LENGTH = 500;
    private static final int MAX_PRICE_TITLE_LENGTH = 40;
    private static final Set<ConversationStep> STEPS = Set.of(
            ConversationStep.AWAITING_VENUE_NAME,
            ConversationStep.AWAITING_VENUE_FIELD,
            ConversationStep.AWAITING_VENUE_PRICE_AMOUNT,
            ConversationStep.AWAITING_VENUE_PRICE_TITLE,
            ConversationStep.AWAITING_BOOKING_NOTE);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final VenueService venues;
    private final VenueBookingService bookings;
    private final VenueNotifier notifier;
    private final BotMessenger messenger;

    VenueInputHandler(ConversationService conversations, MembershipGate membershipGate,
                      TelegramAccountResolver accounts, VenueService venues, VenueBookingService bookings,
                      VenueNotifier notifier, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.venues = venues;
        this.bookings = bookings;
        this.notifier = notifier;
        this.messenger = messenger;
    }

    @Override
    public boolean supports(Update update) {
        Message message = update.message();
        return message != null
                && message.from() != null
                && message.text() != null
                && !message.text().startsWith("/")
                && conversations.find(message.from().id())
                        .map(ConversationState::getStep)
                        .filter(STEPS::contains)
                        .isPresent();
    }

    @Override
    public void handle(Update update) {
        Message message = update.message();
        TelegramUser from = message.from();
        long chatId = message.chat().id();
        if (!membershipGate.admit(from.id(), chatId)) {
            return;
        }
        ConversationState state = conversations.find(from.id()).orElse(null);
        if (state == null) {
            return;
        }
        Long userId = accounts.resolve(from).getId();
        switch (state.getStep()) {
            case AWAITING_VENUE_NAME -> receiveName(from, chatId, userId, message.text());
            case AWAITING_VENUE_FIELD -> receiveField(from, chatId, userId, message.text(), state.getData());
            case AWAITING_VENUE_PRICE_AMOUNT -> receivePriceAmount(from, chatId, message.text(), state.getData());
            case AWAITING_VENUE_PRICE_TITLE -> receivePriceTitle(from, chatId, userId, message.text(), state.getData());
            case AWAITING_BOOKING_NOTE -> receiveNote(from, chatId, userId, message.text(), state.getData());
            default -> { }
        }
    }

    private void receiveName(TelegramUser from, long chatId, Long userId, String raw) {
        String name = raw.strip().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            messenger.sendText(chatId, "اسم باید بین ۱ تا ۱۰۰ حرف باشه. دوباره بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        Optional<Venue> venue = venues.create(userId, name);
        if (venue.isEmpty()) {
            messenger.send(chatId, VenueOwnerScreens.limitReached());
            return;
        }
        messenger.sendText(chatId, "✅ مکان ساخته شد. حالا جزئیاتش رو کامل کن: شهر، قیمت و واحد قیمت، "
                + "بعد عکس‌ها و روزهای آزاد.");
        messenger.send(chatId, VenueOwnerScreens.manage(venue.get(), 0, 0, List.of()));
    }

    private void receiveField(TelegramUser from, long chatId, Long userId, String raw, Map<String, String> data) {
        Optional<Long> venueId = VenueArgs.id(data.getOrDefault(VenueCallbackHandler.VENUE_KEY, ""));
        Optional<VenueField> field = VenueField.fromKey(data.getOrDefault(VenueOwnerCallbackHandler.FIELD_KEY, ""));
        if (venueId.isEmpty() || field.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, VenueOwnerScreens.gone());
            return;
        }
        Optional<Venue> updated;
        try {
            updated = venues.update(venueId.get(), userId, venue -> field.get().applyTo(venue, raw));
        } catch (InvalidVenueInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        if (updated.isEmpty()) {
            messenger.send(chatId, VenueOwnerScreens.gone());
            return;
        }
        messenger.sendText(chatId, "✅ " + field.get().label() + " ذخیره شد.");
        Venue venue = venues.find(venueId.get()).orElse(updated.get());
        messenger.send(chatId, VenueOwnerScreens.manage(venue, venues.photoCount(venue.getId()),
                bookings.pendingCount(venue.getId()), venues.prices(venue.getId())));
    }

    /** The amount of a new price; the unit was picked with a button. Next comes its optional name. */
    private void receivePriceAmount(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        long amount;
        try {
            amount = PriceAmountParser.parse(raw);
        } catch (InvalidPriceInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(VenueOwnerCallbackHandler.AMOUNT_KEY, String.valueOf(amount));
        conversations.begin(from.id(), ConversationStep.AWAITING_VENUE_PRICE_TITLE, next);
        messenger.send(chatId, VenueOwnerScreens.askPriceTitle());
    }

    private void receivePriceTitle(TelegramUser from, long chatId, Long userId, String raw, Map<String, String> data) {
        Optional<Long> venueId = VenueArgs.id(data.getOrDefault(VenueCallbackHandler.VENUE_KEY, ""));
        Optional<VenuePriceUnit> unit = VenuePriceUnit.fromKey(data.getOrDefault(VenueOwnerCallbackHandler.UNIT_KEY, ""));
        Optional<Long> amount = VenueArgs.id(data.getOrDefault(VenueOwnerCallbackHandler.AMOUNT_KEY, ""));
        String title = raw.strip().replaceAll("\\s+", " ");
        if (title.length() > MAX_PRICE_TITLE_LENGTH) {
            messenger.sendText(chatId, "اسم خیلی بلنده. حداکثر ۴۰ حرف بفرست یا «-» بزن.");
            return;
        }
        conversations.clear(from.id());
        if (venueId.isEmpty() || unit.isEmpty() || amount.isEmpty()) {
            messenger.send(chatId, VenueOwnerScreens.gone());
            return;
        }
        Optional<VenuePrice> added = venues.addPrice(venueId.get(), userId, unit.get(),
                title.isEmpty() || title.equals("-") ? null : title, amount.get());
        Optional<Venue> venue = venues.findOwned(venueId.get(), userId);
        if (venue.isEmpty()) {
            messenger.send(chatId, VenueOwnerScreens.gone());
            return;
        }
        messenger.sendText(chatId, added.isPresent() ? "✅ قیمت اضافه شد."
                : "حداکثر " + VenueService.MAX_PRICES + " قیمت می‌تونی داشته باشی. اول یکی رو حذف کن.");
        messenger.send(chatId, VenueOwnerScreens.prices(venue.get(), venues.prices(venue.get().getId())));
    }

    private void receiveNote(TelegramUser from, long chatId, Long userId, String raw, Map<String, String> data) {
        Optional<Long> venueId = VenueArgs.id(data.getOrDefault(VenueCallbackHandler.VENUE_KEY, ""));
        Optional<LocalDate> day = VenueArgs.date(data.getOrDefault(VenueCallbackHandler.DAY_KEY, ""));
        String note = raw.strip();
        if (venueId.isEmpty() || day.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, VenueScreens.gone());
            return;
        }
        if (note.length() > MAX_NOTE_LENGTH) {
            messenger.sendText(chatId, "توضیح خیلی بلنده. حداکثر ۵۰۰ حرف بفرست یا /cancel رو بزن.");
            return;
        }
        VenueBooking booking;
        try {
            booking = bookings.request(venueId.get(), userId, day.get(), note.isEmpty() ? null : note);
        } catch (VenueBookingException e) {
            conversations.clear(from.id());
            messenger.sendText(chatId, refusal(e.reason()));
            messenger.send(chatId, VenueScreens.home());
            return;
        }
        conversations.clear(from.id());
        Venue venue = venues.find(venueId.get()).orElse(null);
        if (venue != null) {
            notifier.requestArrived(venue, booking);
            messenger.send(chatId, VenueScreens.requestSent(venue));
        }
    }

    private static String refusal(VenueBookingException.Reason reason) {
        return switch (reason) {
            case VENUE_NOT_AVAILABLE -> "این مکان دیگه در دسترس نیست.";
            case OWN_VENUE -> "این مکان خودته؛ نمی‌تونی برای خودت درخواست بدی.";
            case DAY_NOT_FREE -> "این روز دیگه آزاد نیست. یک روز دیگه انتخاب کن.";
            case ALREADY_REQUESTED -> "برای این روز قبلاً درخواست دادی و منتظر جواب هستی.";
        };
    }
}
