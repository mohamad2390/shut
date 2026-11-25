package com.shut.telegram.market;

import com.shut.equipment.EquipmentType;
import com.shut.equipment.EquipmentTypeRepository;
import com.shut.market.Listing;
import com.shut.market.ListingCondition;
import com.shut.market.ListingPhoto;
import com.shut.market.ListingStatus;
import com.shut.market.MarketService;
import com.shut.market.Offer;
import com.shut.market.OfferService;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Handles the seller's buttons: their listings, the details of each, photos, status and offers. */
@Component
@TelegramBotEnabled
class MarketOwnerCallbackHandler extends CallbackHandler {

    static final String FIELD_KEY = "field";
    static final String TITLE_KEY = "title";
    static final String PRICE_KEY = "price";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final MarketService market;
    private final OfferService offers;
    private final EquipmentTypeRepository types;
    private final MarketNotifier notifier;
    private final MediaBackupService backups;

    MarketOwnerCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                               ConversationService conversations, TelegramAccountResolver accounts,
                               MarketService market, OfferService offers, EquipmentTypeRepository types,
                               MarketNotifier notifier, MediaBackupService backups) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.market = market;
        this.offers = offers;
        this.types = types;
        this.notifier = notifier;
        this.backups = backups;
    }

    @Override
    protected String prefix() {
        return MarketOwnerCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "mine" -> showMine(context);
            case "new" -> startNew(context);
            case "nc" -> createListing(context);
            case "m" -> withListing(context, listing -> manage(context, listing));
            case "e" -> withListing(context, listing -> startEditing(context, listing));
            case "cond" -> withListing(context, listing -> MarketOwnerScreens.pickCondition(listing.getId(),
                    "وضعیت جنس «" + listing.getTitle() + "» چیه؟"));
            case "sc" -> withListing(context, listing -> setCondition(context, listing));
            case "cat" -> withListing(context, listing -> MarketOwnerScreens.pickCategory(listing,
                    types.findAllByOrderBySortOrderAscIdAsc()));
            case "sk" -> withListing(context, listing -> setCategory(context, listing));
            case "ng" -> withListing(context, listing -> toggleNegotiable(listing));
            case "ss" -> withListing(context, listing -> setStatus(context, listing));
            case "ph" -> withListing(context, listing -> startPhotos(context, listing));
            case "phx" -> withListing(context, this::clearPhotos);
            case "of" -> withListing(context, this::offersScreen);
            case "oa" -> respond(context, true);
            case "od" -> respond(context, false);
            case "del" -> withListing(context, MarketOwnerScreens::confirmDelete);
            case "delok" -> deleteListing(context);
            default -> { }
        }
    }

    private void showMine(CallbackContext context) {
        conversations.clear(context.user().id());
        List<Listing> mine = market.ownedBy(userId(context));
        Map<Long, Long> pending = new HashMap<>();
        mine.forEach(listing -> pending.put(listing.getId(), offers.pendingCount(listing.getId())));
        show(context, MarketOwnerScreens.mine(mine, pending));
    }

    private void startNew(CallbackContext context) {
        long active = market.ownedBy(userId(context)).stream()
                .filter(listing -> listing.getStatus() == ListingStatus.ACTIVE).count();
        if (active >= MarketService.MAX_ACTIVE_LISTINGS_PER_SELLER) {
            show(context, MarketOwnerScreens.limitReached());
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_LISTING_TITLE, Map.of());
        show(context, MarketOwnerScreens.askTitle());
    }

    /** The condition was picked: the title and price waiting in the conversation become a listing. */
    private void createListing(CallbackContext context) {
        Map<String, String> data = conversations.find(context.user().id())
                .filter(state -> state.getStep() == ConversationStep.AWAITING_LISTING_CONDITION)
                .map(state -> state.getData()).orElse(null);
        Optional<ListingCondition> condition = ListingCondition.fromKey(context.arg(1));
        if (data == null || condition.isEmpty()) {
            return;
        }
        conversations.clear(context.user().id());
        long price;
        try {
            price = Long.parseLong(data.getOrDefault(PRICE_KEY, ""));
        } catch (NumberFormatException e) {
            show(context, MarketOwnerScreens.gone());
            return;
        }
        Optional<Listing> created = market.create(userId(context), data.getOrDefault(TITLE_KEY, ""), condition.get(),
                price, listing -> { });
        if (created.isEmpty()) {
            show(context, MarketOwnerScreens.limitReached());
            return;
        }
        messenger.sendText(context.chatId(), "✅ آگهی ثبت شد و توی بازار دیده می‌شه. برای جذاب‌تر شدنش شهر، برند، مدل، "
                + "توضیحات و عکس اضافه کن.");
        messenger.send(context.chatId(), manageScreen(created.get()));
    }

    private Screen manage(CallbackContext context, Listing listing) {
        conversations.clear(context.user().id());
        return manageScreen(listing);
    }

    private Screen manageScreen(Listing listing) {
        String category = listing.getCategoryId() == null ? null
                : types.findById(listing.getCategoryId()).map(EquipmentType::getName).orElse(null);
        return MarketOwnerScreens.manage(listing, category, market.photoCount(listing.getId()),
                offers.pendingCount(listing.getId()));
    }

    private Screen startEditing(CallbackContext context, Listing listing) {
        Optional<ListingField> field = ListingField.fromKey(context.arg(2));
        if (field.isEmpty()) {
            return manageScreen(listing);
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_LISTING_FIELD,
                Map.of(MarketCallbackHandler.LISTING_KEY, String.valueOf(listing.getId()), FIELD_KEY, field.get().name()));
        return new Screen(field.get().prompt() + "\n\nبرای انصراف /cancel رو بزن.", new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("انصراف", MarketOwnerCallbacks.manage(listing.getId())))
                .build());
    }

    private Screen setCondition(CallbackContext context, Listing listing) {
        ListingCondition.fromKey(context.arg(2))
                .ifPresent(condition -> market.update(listing.getId(), listing.getSellerId(), l -> l.setCondition(condition)));
        return manageScreen(reload(listing));
    }

    private Screen setCategory(CallbackContext context, Listing listing) {
        long categoryId;
        try {
            categoryId = Long.parseLong(context.arg(2));
        } catch (NumberFormatException e) {
            return manageScreen(listing);
        }
        if (categoryId == 0 || types.existsById(categoryId)) {
            market.update(listing.getId(), listing.getSellerId(), l -> l.setCategoryId(categoryId == 0 ? null : categoryId));
        }
        return manageScreen(reload(listing));
    }

    private Screen toggleNegotiable(Listing listing) {
        market.update(listing.getId(), listing.getSellerId(), l -> l.setNegotiable(!l.isNegotiable()));
        return manageScreen(reload(listing));
    }

    private Screen setStatus(CallbackContext context, Listing listing) {
        Optional<ListingStatus> status = ListingStatus.fromKey(context.arg(2));
        if (status.isEmpty()) {
            return manageScreen(listing);
        }
        if (market.setStatus(listing.getId(), listing.getSellerId(), status.get()).isEmpty()) {
            messenger.sendText(context.chatId(), "به سقف آگهی‌های فعالت رسیدی؛ اول یکی دیگه رو ببند یا فروخته‌شده کن.");
        }
        return manageScreen(reload(listing));
    }

    private Screen startPhotos(CallbackContext context, Listing listing) {
        conversations.begin(context.user().id(), ConversationStep.AWAITING_LISTING_PHOTO,
                Map.of(MarketCallbackHandler.LISTING_KEY, String.valueOf(listing.getId())));
        return MarketOwnerScreens.addPhotos(listing, market.photoCount(listing.getId()));
    }

    private Screen clearPhotos(Listing listing) {
        market.clearPhotos(listing.getId(), listing.getSellerId()).stream()
                .map(ListingPhoto::getBackupMessageId)
                .forEach(backups::remove);
        return manageScreen(listing);
    }

    private Screen offersScreen(Listing listing) {
        List<MarketOwnerScreens.OfferLine> lines = offers.pending(listing.getId(), listing.getSellerId()).stream()
                .map(offer -> new MarketOwnerScreens.OfferLine(offer, notifier.nameOf(offer.getBuyerId()))).toList();
        return MarketOwnerScreens.offers(listing, lines);
    }

    /** Accepts or declines an offer, tells the buyer, and shows the seller what is still waiting. */
    private void respond(CallbackContext context, boolean accept) {
        Long sellerId = userId(context);
        Optional<Offer> answered = parse(context.arg(1)).flatMap(id -> offers.respond(id, sellerId, accept));
        if (answered.isEmpty()) {
            messenger.sendText(context.chatId(), "این پیشنهاد دیگه منتظر جواب نیست.");
            return;
        }
        Optional<Listing> listing = market.findOwned(answered.get().getListingId(), sellerId);
        listing.ifPresent(found -> notifier.offerAnswered(found, answered.get()));
        show(context, listing.map(this::offersScreen).orElseGet(MarketOwnerScreens::gone));
    }

    private void deleteListing(CallbackContext context) {
        Long sellerId = userId(context);
        parse(context.arg(1)).flatMap(id -> market.delete(id, sellerId))
                .ifPresent(photos -> photos.stream().map(ListingPhoto::getBackupMessageId).forEach(backups::remove));
        showMine(context);
    }

    private Listing reload(Listing listing) {
        return market.find(listing.getId()).orElse(listing);
    }

    /** Runs the action on the listing in the callback if it is the user's, and shows what it returns. */
    private void withListing(CallbackContext context, Function<Listing, Screen> action) {
        Screen screen = parse(context.arg(1))
                .flatMap(id -> market.findOwned(id, userId(context)))
                .map(action)
                .orElseGet(MarketOwnerScreens::gone);
        show(context, screen);
    }

    private static Optional<Long> parse(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Long userId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
