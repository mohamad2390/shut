package com.shut.telegram.pricing;

import com.shut.pricing.PricingService;
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

import java.util.Map;
import java.util.Optional;

/** Handles every button in the price menu section. */
@Component
@TelegramBotEnabled
class PricingCallbackHandler extends CallbackHandler {

    static final String TITLE_KEY = "title";
    static final String UNIT_KEY = "unit";

    private final TelegramAccountResolver accounts;
    private final PricingService pricing;
    private final ConversationService conversations;
    private final BotMessenger messenger;
    private final PricingScreenProvider screens;

    PricingCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate,
                           TelegramAccountResolver accounts, PricingService pricing,
                           ConversationService conversations, BotMessenger messenger,
                           PricingScreenProvider screens) {
        super(telegram, membershipGate);
        this.accounts = accounts;
        this.pricing = pricing;
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
            case "unit" -> chooseUnit(context);
            case "manage" -> showDeleteMenu(context);
            case "del" -> deleteItem(context);
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
        conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_TITLE, Map.of());
        messenger.show(context.chatId(), context.messageId(), PricingScreens.askTitle());
    }

    /** The user picked a unit for the service whose name they just sent; next comes the amount. */
    private void chooseUnit(CallbackContext context) {
        Optional<ConversationState> state = conversations.find(context.user().id())
                .filter(s -> s.getStep() == ConversationStep.AWAITING_PRICE_UNIT);
        String title = state.map(s -> s.getData().get(TITLE_KEY)).orElse(null);
        Optional<PricingUnit> unit = PricingUnit.fromKey(context.arg(1));
        if (title == null || unit.isEmpty()) {
            return;
        }
        conversations.begin(context.user().id(), ConversationStep.AWAITING_PRICE_AMOUNT,
                Map.of(TITLE_KEY, title, UNIT_KEY, unit.get().name()));
        messenger.show(context.chatId(), context.messageId(), PricingScreens.askAmount(title, unit.get()));
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
