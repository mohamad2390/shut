package com.shut.telegram.rental;

import com.shut.pricing.PricingService;
import com.shut.rental.RentalItem;
import com.shut.rental.RentalService;
import com.shut.rental.RentalShop;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.dispatch.UpdateHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.Message;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.model.Update;
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;
import com.shut.user.entity.User;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receives the text typed for rental: registration answers, a detail, an item's answers or price, a search. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class RentalInputHandler implements UpdateHandler {

    static final int MAX_DESCRIPTION = 500;
    static final int MAX_QUERY = 60;
    private static final int RESULTS = 10;
    private static final String PRICE_KEY = "price";
    private static final Set<ConversationStep> STEPS = Set.of(ConversationStep.AWAITING_RENTAL_WIZARD,
            ConversationStep.AWAITING_RENTAL_FIELD, ConversationStep.AWAITING_RENTAL_ITEM,
            ConversationStep.AWAITING_RENTAL_SEARCH);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final RentalService rentals;
    private final BotMessenger messenger;

    RentalInputHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, RentalService rentals, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.rentals = rentals;
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
                        .map(ConversationState::getStep).filter(STEPS::contains).isPresent();
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
        User user = accounts.resolve(from);
        switch (state.getStep()) {
            case AWAITING_RENTAL_WIZARD -> register(from, chatId, user, message.text(), state.getData());
            case AWAITING_RENTAL_FIELD -> editField(from, chatId, user, message.text(), state.getData());
            case AWAITING_RENTAL_ITEM -> item(from, chatId, user, message.text(), state.getData());
            case AWAITING_RENTAL_SEARCH -> search(from, chatId, message.text());
            default -> { }
        }
    }

    /** Each answer is checked and kept in the conversation; the shop is created after the last one. */
    private void register(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        Optional<RentalField> field = RentalField.fromKey(data.get(RentalOwnerCallbackHandler.FIELD_KEY));
        if (field.isEmpty()) {
            conversations.clear(from.id());
            return;
        }
        String value;
        try {
            value = field.get().value(raw);
        } catch (InvalidRentalInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(field.get().name(), value);
        Optional<RentalField> following = field.get().nextInRegistration();
        if (following.isPresent()) {
            next.put(RentalOwnerCallbackHandler.FIELD_KEY, following.get().name());
            conversations.begin(from.id(), ConversationStep.AWAITING_RENTAL_WIZARD, next);
            messenger.send(chatId, RentalOwnerScreens.ask(following.get(), true));
            return;
        }
        conversations.clear(from.id());
        if (rentals.create(user, next.get(RentalField.NAME.name())).isEmpty()) {
            messenger.sendText(chatId, "قبلاً یک مغازه ثبت کردی.");
            return;
        }
        rentals.update(user.getId(), shop -> {
            for (RentalField each : RentalField.REGISTRATION) {
                each.applyTo(shop, next.get(each.name()));
            }
        });
        messenger.sendText(chatId, "✅ مغازه ثبت شد. برای نمایش به همه، حداقل یک مورد قابل اجاره با قیمت روزانه اضافه کن.");
        messenger.send(chatId, manage(user));
    }

    private void editField(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        Optional<RentalField> field = RentalField.fromKey(data.get(RentalOwnerCallbackHandler.FIELD_KEY));
        if (field.isEmpty()) {
            conversations.clear(from.id());
            return;
        }
        Optional<RentalShop> updated;
        try {
            updated = rentals.update(user.getId(), shop -> field.get().applyTo(shop, raw));
        } catch (InvalidRentalInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        if (updated.isEmpty()) {
            messenger.send(chatId, RentalOwnerScreens.gone());
            return;
        }
        messenger.sendText(chatId, "✅ " + field.get().label() + " ذخیره شد.");
        messenger.send(chatId, manage(user));
    }

    /**
     * A new item (its product already picked from the catalog) takes two answers, its daily price and a description;
     * "EDIT_PRICE" changes the price of an existing one.
     */
    private void item(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        String text = raw.strip().replaceAll("[ \\t]+", " ");
        Map<String, String> next = new HashMap<>(data);
        switch (data.getOrDefault(RentalOwnerCallbackHandler.STAGE_KEY, "")) {
            case "PRICE" -> price(chatId, text).ifPresent(amount -> {
                next.put(PRICE_KEY, String.valueOf(amount));
                next.put(RentalOwnerCallbackHandler.STAGE_KEY, "DESCRIPTION");
                conversations.begin(from.id(), ConversationStep.AWAITING_RENTAL_ITEM, next);
                messenger.send(chatId, RentalOwnerScreens.askItemDescription());
            });
            case "DESCRIPTION" -> {
                if (text.length() > MAX_DESCRIPTION) {
                    messenger.sendText(chatId, "توضیح خیلی بلنده؛ حداکثر ۵۰۰ حرف. دوباره بفرست یا /cancel رو بزن.");
                    return;
                }
                addItem(from, chatId, user, data, text.equals("-") || text.isEmpty() ? null : text);
            }
            case "EDIT_PRICE" -> price(chatId, text).ifPresent(amount -> changePrice(from, chatId, user, data, amount));
            default -> conversations.clear(from.id());
        }
    }

    /** The amount typed, or empty after telling the member what is wrong with it. */
    private Optional<Long> price(long chatId, String text) {
        try {
            long amount = PriceAmountParser.parse(text);
            if (amount <= 0 || amount > PricingService.MAX_AMOUNT) {
                throw new InvalidPriceInputException("قیمت باید بیشتر از صفر باشه.");
            }
            return Optional.of(amount);
        } catch (InvalidPriceInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return Optional.empty();
        }
    }

    private void addItem(TelegramUser from, long chatId, User user, Map<String, String> data, String description) {
        conversations.clear(from.id());
        Optional<RentalItem> added;
        try {
            added = rentals.addItem(user.getId(), Long.parseLong(data.getOrDefault(RentalOwnerCallbackHandler.MODEL_KEY, "")),
                    description, Long.parseLong(data.getOrDefault(PRICE_KEY, "")));
        } catch (NumberFormatException e) {
            added = Optional.empty();
        }
        if (added.isPresent()) {
            messenger.sendText(chatId, "✅ اضافه شد. حالا می‌تونی عکس هم بذاری.");
            messenger.send(chatId, RentalOwnerScreens.item(added.get(), 0));
            return;
        }
        messenger.sendText(chatId, "نتونستم اضافه کنم؛ شاید قبلاً توی مغازه‌ات بوده یا به سقف ۴۰ مورد رسیدی.");
        rentals.ownedBy(user.getId()).ifPresent(shop ->
                messenger.send(chatId, RentalOwnerScreens.items(rentals.items(shop.getId()))));
    }

    private void changePrice(TelegramUser from, long chatId, User user, Map<String, String> data, long amount) {
        conversations.clear(from.id());
        Optional<RentalItem> updated;
        try {
            updated = rentals.updateItem(user.getId(), Long.parseLong(data.getOrDefault(RentalOwnerCallbackHandler.ITEM_KEY, "")),
                    item -> item.setDailyPrice(amount));
        } catch (NumberFormatException e) {
            updated = Optional.empty();
        }
        messenger.send(chatId, updated.map(item -> RentalOwnerScreens.item(item, rentals.photoCount(item.getId())))
                .orElseGet(RentalOwnerScreens::gone));
    }

    private void search(TelegramUser from, long chatId, String raw) {
        String query = raw.strip().replaceAll("\\s+", " ");
        if (query.isEmpty() || query.length() > MAX_QUERY) {
            messenger.sendText(chatId, "یک اسم کوتاه‌تر (حداکثر ۶۰ حرف) بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        messenger.send(chatId, RentalScreens.results(query, rentals.search(query, 0, RESULTS)));
    }

    private Screen manage(User user) {
        RentalShop shop = rentals.ownedBy(user.getId()).orElseThrow();
        return RentalOwnerScreens.manage(shop, rentals.isListed(shop), rentals.items(shop.getId()).size(),
                rentals.availableCount(shop.getId()));
    }
}
