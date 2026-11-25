package com.shut.telegram.market;

import com.shut.market.Listing;
import com.shut.market.ListingPhoto;
import com.shut.market.MarketService;
import com.shut.market.Offer;
import com.shut.market.OfferService;
import com.shut.equipment.EquipmentTypeRepository;
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
import com.shut.telegram.model.SendMediaRequest;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles the buttons for browsing the market, looking at a listing and starting an offer. */
@Component
@TelegramBotEnabled
class MarketCallbackHandler extends CallbackHandler {

    static final String LISTING_KEY = "listing";
    static final String AMOUNT_KEY = "amount";
    private static final int PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(MarketCallbackHandler.class);

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final UserService users;
    private final MarketService market;
    private final OfferService offers;
    private final EquipmentTypeRepository types;
    private final MarketNotifier notifier;

    MarketCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          ConversationService conversations, TelegramAccountResolver accounts, UserService users,
                          MarketService market, OfferService offers, EquipmentTypeRepository types,
                          MarketNotifier notifier) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.users = users;
        this.market = market;
        this.offers = offers;
        this.types = types;
        this.notifier = notifier;
    }

    @Override
    protected String prefix() {
        return MarketCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> showFresh(context, MarketScreens.home());
            case "list" -> showList(context);
            case "l" -> showFresh(context, publicListing(context.arg(1)).map(this::listingPage).orElseGet(MarketScreens::gone));
            case "ph" -> sendPhotos(context);
            case "of" -> startOffer(context);
            case "ask" -> chooseAskingPrice(context);
            case "skip" -> sendOffer(context);
            case "mo" -> showMyOffers(context);
            default -> { }
        }
    }

    private void showList(CallbackContext context) {
        Page<Listing> page = market.search(null, null, null, page(context.arg(1)), PAGE_SIZE);
        show(context, MarketScreens.list(page));
    }

    private Screen listingPage(Listing listing) {
        return MarketScreens.listing(listing, categoryName(listing), market.photoCount(listing.getId()),
                sellerName(listing));
    }

    private String categoryName(Listing listing) {
        return listing.getCategoryId() == null ? null
                : types.findById(listing.getCategoryId()).map(type -> type.getName()).orElse(null);
    }

    private String sellerName(Listing listing) {
        var seller = users.findById(listing.getSellerId());
        String name = ((seller.getFirstName() == null ? "" : seller.getFirstName()) + " "
                + (seller.getLastName() == null ? "" : seller.getLastName())).trim();
        return name.isEmpty() ? null : name;
    }

    /** The photos go out one by one, then the listing page again so the buttons are at the bottom. */
    private void sendPhotos(CallbackContext context) {
        Optional<Listing> listing = publicListing(context.arg(1));
        if (listing.isEmpty()) {
            show(context, MarketScreens.gone());
            return;
        }
        List<ListingPhoto> photos = market.photos(listing.get().getId());
        if (photos.isEmpty()) {
            messenger.sendText(context.chatId(), "این آگهی هنوز عکسی نداره.");
            return;
        }
        photos.forEach(photo -> sendPhoto(context.chatId(), photo));
        messenger.send(context.chatId(), listingPage(listing.get()));
    }

    /** One broken file must not stop the rest from being sent. */
    private void sendPhoto(long chatId, ListingPhoto photo) {
        try {
            telegram.sendMedia(SendMediaRequest.photo(chatId, photo.getFileId(), null, null));
        } catch (TelegramApiException e) {
            log.warn("Could not send listing photo {}: {}", photo.getId(), e.getMessage());
        }
    }

    /** "Buy / offer" was tapped: ask for the amount. */
    private void startOffer(CallbackContext context) {
        Optional<Listing> listing = offerableListing(context);
        if (listing.isEmpty()) {
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_OFFER_AMOUNT,
                Map.of(LISTING_KEY, String.valueOf(listing.get().getId())));
        show(context, MarketScreens.askAmount(listing.get()));
    }

    /** "I take it at the asking price" was tapped: no amount, go on to the note. */
    private void chooseAskingPrice(CallbackContext context) {
        Optional<Listing> listing = offerableListing(context);
        if (listing.isEmpty()) {
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_OFFER_NOTE,
                Map.of(LISTING_KEY, String.valueOf(listing.get().getId())));
        show(context, MarketScreens.askNote(listing.get()));
    }

    /** "No message" was tapped: the offer goes out with the amount (if any) the conversation holds. */
    private void sendOffer(CallbackContext context) {
        Map<String, String> data = conversations.find(context.user().id())
                .filter(state -> state.getStep() == ConversationStep.AWAITING_OFFER_NOTE)
                .map(state -> new HashMap<>(state.getData())).orElse(null);
        if (data == null) {
            return;
        }
        MarketOffering.send(conversations, offers, market, notifier, messenger, context.user().id(), context.chatId(),
                accounts.resolve(context.user()).getId(), data, null);
    }

    /** The listing in the callback if it is shown to others and not the user's own; otherwise the user is told. */
    private Optional<Listing> offerableListing(CallbackContext context) {
        Optional<Listing> listing = publicListing(context.arg(1));
        if (listing.isEmpty()) {
            show(context, MarketScreens.gone());
            return listing;
        }
        if (listing.get().getSellerId().equals(accounts.resolve(context.user()).getId())) {
            messenger.sendText(context.chatId(), "این آگهی خودته؛ نمی‌تونی برای خودت پیشنهاد بدی.");
            return Optional.empty();
        }
        return listing;
    }

    private void showMyOffers(CallbackContext context) {
        List<Offer> latest = offers.latestOf(accounts.resolve(context.user()).getId());
        Map<Long, Listing> byId = new HashMap<>();
        latest.forEach(offer -> market.find(offer.getListingId()).ifPresent(listing -> byId.put(listing.getId(), listing)));
        show(context, MarketScreens.myOffers(latest, byId));
    }

    private Optional<Listing> publicListing(String rawId) {
        try {
            return market.findPublic(Long.parseLong(rawId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static int page(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showFresh(CallbackContext context, Screen screen) {
        conversations.clear(context.user().id());
        show(context, screen);
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
