package com.shut.telegram.pricing;

import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingService;
import com.shut.pricing.ServiceType;
import com.shut.pricing.PricingUnit;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles every button in the price menu section. */
@Component
@TelegramBotEnabled
class PricingCallbackHandler extends CallbackHandler {

    static final String SERVICE_KEY = "serviceId";
    /** The name the member gave a price of a service that lets them name their own. Absent if they skipped it. */
    static final String TITLE_KEY = "title";
    static final String UNIT_KEY = "unit";
    static final String ITEM_KEY = "itemId";

    private final TelegramAccountResolver accounts;
    private final PricingService pricing;
    private final PricingCatalog catalog;
    private final ConversationService conversations;
    private final BotMessenger messenger;
    private final PricingScreenProvider screens;

    PricingCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                           TelegramAccountResolver accounts, PricingService pricing,
                           PricingCatalog catalog, ConversationService conversations, BotMessenger messenger,
                           PricingScreenProvider screens) {
        super(telegram, membershipGate);
        this.accounts = accounts;
        this.pricing = pricing;
        this.catalog = catalog;
        this.conversations = conversations;
        this.messenger = messenger;
        this.screens = screens;
    }

    @Override
    protected String prefix() {
        return PricingCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "add" -> beginAdding(context);
            case "svc" -> chooseService(context);
            case "skip" -> skipName(context);
            case "unit" -> chooseUnit(context);
            case "manage" -> showDeleteMenu(context);
            case "del" -> deleteItem(context);
            case "edit" -> showEditMenu(context);
            case "item" -> parseItemId(context).ifPresent(id -> showItem(context, id));
            case "amt" -> parseItemId(context).ifPresent(id -> askNewAmount(context, id));
            case "units" -> parseItemId(context).ifPresent(id -> showUnitMenu(context, id));
            case "su" -> parseItemId(context).ifPresent(id -> changeUnit(context, id));
            case "cancel" -> backToList(context);
            default -> { }
        }
    }

    private Long userId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void beginAdding(CallbackContext context) {
        if (pricing.isFull(userId(context))) {
            messenger.sendText(context.chatId(), "منوی قیمتت پره. برای اضافه کردن، اول یکی از خدمت‌ها رو حذف کن.");
            return;
        }
        conversations.clear(context.user().id());
        List<ServiceType> services = catalog.services();
        if (services.isEmpty()) {
            messenger.sendText(context.chatId(), "هنوز خدمتی برای انتخاب تعریف نشده.");
            return;
        }
        messenger.show(context.chatId(), context.messageId(), PricingScreens.askService(services));
    }

    /** The user picked a service from the list; next comes what the price is charged per. */
    private void chooseService(CallbackContext context) {
        Optional<ServiceType> service = parseItemId(context).flatMap(catalog::service);
        if (service.isEmpty()) {
            messenger.show(context.chatId(), context.messageId(), PricingScreens.askService(catalog.services()));
            return;
        }
        Map<String, String> data = Map.of(SERVICE_KEY, Long.toString(service.get().getId()));
        if (service.get().asksName()) {
            conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_TITLE, data);
            messenger.show(context.chatId(), context.messageId(), PricingScreens.askName(service.get()));
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_UNIT, data);
        messenger.show(context.chatId(), context.messageId(),
                PricingScreens.askUnit(service.get(), service.get().getName(), catalog.unitIcons()));
    }

    /** The member chose not to name the price, so it is called after the service. */
    private void skipName(CallbackContext context) {
        Optional<ConversationState> state = conversations.find(context.user().id())
                .filter(s -> s.getStep() == ConversationStep.AWAITING_PRICE_TITLE);
        Optional<ServiceType> service = state.map(s -> s.getData().get(SERVICE_KEY))
                .flatMap(PricingCallbackHandler::parseId)
                .flatMap(catalog::service);
        if (service.isEmpty()) {
            messenger.show(context.chatId(), context.messageId(), PricingScreens.askService(catalog.services()));
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_UNIT,
                Map.of(SERVICE_KEY, Long.toString(service.get().getId())));
        messenger.show(context.chatId(), context.messageId(),
                PricingScreens.askUnit(service.get(), service.get().getName(), catalog.unitIcons()));
    }

    /** The user picked a unit for the chosen service; next comes the amount. */
    private void chooseUnit(CallbackContext context) {
        Optional<ConversationState> state = conversations.find(context.user().id())
                .filter(s -> s.getStep() == ConversationStep.AWAITING_PRICE_UNIT);
        Optional<ServiceType> service = state.map(s -> s.getData().get(SERVICE_KEY))
                .flatMap(PricingCallbackHandler::parseId)
                .flatMap(catalog::service);
        Optional<PricingUnit> unit = PricingUnit.fromKey(context.arg(1));
        if (unit.isEmpty()) {
            return;
        }
        if (service.isEmpty()) {
            messenger.show(context.chatId(), context.messageId(), PricingScreens.askService(catalog.services()));
            return;
        }
        Map<String, String> data = new HashMap<>(state.get().getData());
        data.put(UNIT_KEY, unit.get().name());
        conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_AMOUNT, data);
        messenger.show(context.chatId(), context.messageId(),
                PricingScreens.askAmount(service.get(), displayName(service.get(), data), unit.get()));
    }

    private void showEditMenu(CallbackContext context) {
        conversations.clear(context.user().id());
        messenger.show(context.chatId(), context.messageId(), PricingScreens.editMenu(pricing.list(userId(context))));
    }

    private void showItem(CallbackContext context, long itemId) {
        conversations.clear(context.user().id());
        pricing.findOwned(itemId, userId(context)).ifPresentOrElse(
                item -> messenger.show(context.chatId(), context.messageId(), PricingScreens.item(item)),
                () -> backToList(context));
    }

    private void askNewAmount(CallbackContext context, long itemId) {
        pricing.findOwned(itemId, userId(context)).ifPresentOrElse(item -> {
            conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_EDIT_AMOUNT,
                    Map.of(ITEM_KEY, Long.toString(itemId)));
            messenger.show(context.chatId(), context.messageId(), PricingScreens.askNewAmount(item));
        }, () -> backToList(context));
    }

    private void showUnitMenu(CallbackContext context, long itemId) {
        pricing.findOwned(itemId, userId(context)).ifPresentOrElse(
                item -> messenger.show(context.chatId(), context.messageId(), PricingScreens.unitMenu(item, catalog.unitIcons())),
                () -> backToList(context));
    }

    private void changeUnit(CallbackContext context, long itemId) {
        Optional<PricingUnit> unit = PricingUnit.fromKey(context.arg(2));
        if (unit.isEmpty()) {
            return;
        }
        pricing.updateUnit(itemId, userId(context), unit.get()).ifPresentOrElse(
                item -> messenger.show(context.chatId(), context.messageId(), PricingScreens.item(item)),
                () -> backToList(context));
    }

    /** What the price is called while it is being added: the member's own name, or the name of the service. */
    static String displayName(ServiceType service, Map<String, String> data) {
        String title = data.get(TITLE_KEY);
        return title == null || title.isBlank() ? service.getName() : title;
    }

    private static Optional<Long> parseItemId(CallbackContext context) {
        return parseId(context.arg(1));
    }

    static Optional<Long> parseId(String text) {
        try {
            return Optional.of(Long.parseLong(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void showDeleteMenu(CallbackContext context) {
        messenger.show(context.chatId(), context.messageId(), PricingScreens.deleteMenu(pricing.list(userId(context))));
    }

    private void deleteItem(CallbackContext context) {
        try {
            pricing.deleteOwned(Long.parseLong(context.arg(1)), userId(context));
        } catch (NumberFormatException ignored) {
            // a malformed button; just show the list again
        }
        backToList(context);
    }

    private void backToList(CallbackContext context) {
        conversations.clear(context.user().id());
        messenger.show(context.chatId(), context.messageId(), screens.home(context.user()));
    }
}
