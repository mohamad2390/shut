package com.shut.telegram.market;

import com.shut.market.Listing;
import com.shut.market.MarketService;
import com.shut.market.OfferService;
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
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receives the text typed for the market: a new listing's title and price, one detail, an offer's amount or note. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class MarketInputHandler implements UpdateHandler {

    private static final int MAX_NOTE_LENGTH = 500;
    private static final Set<ConversationStep> STEPS = Set.of(
            ConversationStep.AWAITING_LISTING_TITLE,
            ConversationStep.AWAITING_LISTING_PRICE,
            ConversationStep.AWAITING_LISTING_FIELD,
            ConversationStep.AWAITING_OFFER_AMOUNT,
            ConversationStep.AWAITING_OFFER_NOTE);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final MarketService market;
    private final OfferService offers;
    private final MarketNotifier notifier;
    private final BotMessenger messenger;

    MarketInputHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, MarketService market, OfferService offers,
                       MarketNotifier notifier, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.market = market;
        this.offers = offers;
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
            case AWAITING_LISTING_TITLE -> receiveTitle(from, chatId, message.text());
            case AWAITING_LISTING_PRICE -> receivePrice(from, chatId, message.text(), state.getData());
            case AWAITING_LISTING_FIELD -> receiveField(from, chatId, userId, message.text(), state.getData());
            case AWAITING_OFFER_AMOUNT -> receiveAmount(from, chatId, message.text(), state.getData());
            case AWAITING_OFFER_NOTE -> receiveNote(from, chatId, userId, message.text(), state.getData());
            default -> { }
        }
    }

    private void receiveTitle(TelegramUser from, long chatId, String raw) {
        String title;
        try {
            title = ListingField.title(raw);
        } catch (InvalidListingInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.begin(from.id(), ConversationStep.AWAITING_LISTING_PRICE,
                Map.of(MarketOwnerCallbackHandler.TITLE_KEY, title));
        messenger.send(chatId, MarketOwnerScreens.askPrice(title));
    }

    private void receivePrice(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        long price;
        try {
            price = ListingField.price(raw);
        } catch (InvalidListingInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(MarketOwnerCallbackHandler.PRICE_KEY, String.valueOf(price));
        conversations.begin(from.id(), ConversationStep.AWAITING_LISTING_CONDITION, next);
        messenger.send(chatId, MarketOwnerScreens.pickCondition(null, "وضعیت جنس چیه؟"));
    }

    private void receiveField(TelegramUser from, long chatId, Long userId, String raw, Map<String, String> data) {
        Optional<Long> listingId = id(data.get(MarketCallbackHandler.LISTING_KEY));
        Optional<ListingField> field = ListingField.fromKey(data.getOrDefault(MarketOwnerCallbackHandler.FIELD_KEY, ""));
        if (listingId.isEmpty() || field.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, MarketOwnerScreens.gone());
            return;
        }
        Optional<Listing> updated;
        try {
            updated = market.update(listingId.get(), userId, listing -> field.get().applyTo(listing, raw));
        } catch (InvalidListingInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        if (updated.isEmpty()) {
            messenger.send(chatId, MarketOwnerScreens.gone());
            return;
        }
        messenger.sendText(chatId, "✅ " + field.get().label() + " ذخیره شد.");
        Listing listing = market.find(listingId.get()).orElse(updated.get());
        messenger.send(chatId, MarketOwnerScreens.manage(listing, null, market.photoCount(listing.getId()),
                offers.pendingCount(listing.getId())));
    }

    /** The amount offered; next comes the note. */
    private void receiveAmount(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        Optional<Listing> listing = id(data.get(MarketCallbackHandler.LISTING_KEY)).flatMap(market::findPublic);
        if (listing.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, MarketScreens.gone());
            return;
        }
        long amount;
        try {
            amount = PriceAmountParser.parse(raw);
        } catch (InvalidPriceInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(MarketCallbackHandler.AMOUNT_KEY, String.valueOf(amount));
        conversations.begin(from.id(), ConversationStep.AWAITING_OFFER_NOTE, next);
        messenger.send(chatId, MarketScreens.askNote(listing.get()));
    }

    private void receiveNote(TelegramUser from, long chatId, Long userId, String raw, Map<String, String> data) {
        String note = raw.strip();
        if (note.length() > MAX_NOTE_LENGTH) {
            messenger.sendText(chatId, "پیام خیلی بلنده. حداکثر ۵۰۰ حرف بفرست یا /cancel رو بزن.");
            return;
        }
        MarketOffering.send(conversations, offers, market, notifier, messenger, from.id(), chatId, userId, data,
                note.isEmpty() ? null : note);
    }

    private static Optional<Long> id(String raw) {
        try {
            return raw == null ? Optional.empty() : Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
