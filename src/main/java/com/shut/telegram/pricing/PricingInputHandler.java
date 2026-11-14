package com.shut.telegram.pricing;

import com.shut.pricing.PriceLimitReachedException;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingService;
import com.shut.pricing.ServiceType;
import com.shut.pricing.PricingUnit;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receives the price a user types while adding a service or changing a price. The service itself is picked from a list. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class PricingInputHandler implements UpdateHandler {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final Set<ConversationStep> PRICING_STEPS = Set.of(
            ConversationStep.AWAITING_PRICE_TITLE,
            ConversationStep.AWAITING_PRICE_UNIT,
            ConversationStep.AWAITING_PRICE_AMOUNT,
            ConversationStep.AWAITING_PRICE_EDIT_AMOUNT);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final PricingService pricing;
    private final PricingCatalog catalog;
    private final PricingScreenProvider screens;
    private final BotMessenger messenger;

    PricingInputHandler(ConversationService conversations, MembershipGate membershipGate,
                        TelegramAccountResolver accounts, PricingService pricing,
                        PricingCatalog catalog, PricingScreenProvider screens, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.pricing = pricing;
        this.catalog = catalog;
        this.screens = screens;
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
                        .filter(PRICING_STEPS::contains)
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

        switch (state.getStep()) {
            case AWAITING_PRICE_TITLE -> receiveTitle(from, chatId, message.text(), state.getData());
            case AWAITING_PRICE_UNIT -> remindUnit(from, chatId, state.getData());
            case AWAITING_PRICE_AMOUNT -> receiveAmount(from, chatId, message.text(), state.getData());
            case AWAITING_PRICE_EDIT_AMOUNT -> receiveNewAmount(from, chatId, message.text(), state.getData());
            default -> { }
        }
    }

    private void receiveNewAmount(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        long itemId;
        try {
            itemId = Long.parseLong(data.getOrDefault(PricingCallbackHandler.ITEM_KEY, ""));
        } catch (NumberFormatException e) {
            conversations.clear(from.id());
            return;
        }

        long amount;
        try {
            amount = PriceAmountParser.parse(raw);
        } catch (InvalidPriceInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }

        boolean updated = pricing.updateAmount(itemId, accounts.resolve(from).getId(), amount).isPresent();
        conversations.clear(from.id());
        messenger.sendText(chatId, updated ? "✅ قیمت عوض شد." : "این خدمت دیگه وجود نداره.");
        messenger.send(chatId, screens.home(from));
    }

    /** The optional name of a price; an empty or too long one is asked for again. */
    private void receiveTitle(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        Optional<ServiceType> service = chosenService(data);
        if (service.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, screens.home(from));
            return;
        }
        String title = raw.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            messenger.sendText(chatId, "اسم خیلی بلنده. حداکثر ۱۰۰ حرف بفرست یا «رد کردن» رو بزن.");
            return;
        }
        Map<String, String> next = title.isEmpty()
                ? Map.of(PricingCallbackHandler.SERVICE_KEY, data.get(PricingCallbackHandler.SERVICE_KEY))
                : Map.of(PricingCallbackHandler.SERVICE_KEY, data.get(PricingCallbackHandler.SERVICE_KEY),
                        PricingCallbackHandler.TITLE_KEY, title);
        conversations.begin(from.id(), ConversationStep.AWAITING_PRICE_UNIT, next);
        messenger.send(chatId, PricingScreens.askUnit(service.get(),
                PricingCallbackHandler.displayName(service.get(), next), catalog.unitIcons()));
    }

    /** A text arrived where a button press was expected: show the unit buttons again. */
    private void remindUnit(TelegramUser from, long chatId, Map<String, String> data) {
        Optional<ServiceType> service = chosenService(data);
        if (service.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, screens.home(from));
            return;
        }
        messenger.send(chatId, PricingScreens.askUnit(service.get(),
                PricingCallbackHandler.displayName(service.get(), data), catalog.unitIcons()));
    }

    private Optional<ServiceType> chosenService(Map<String, String> data) {
        return PricingCallbackHandler.parseId(data.getOrDefault(PricingCallbackHandler.SERVICE_KEY, ""))
                .flatMap(catalog::service);
    }

    private void receiveAmount(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        Optional<ServiceType> service = chosenService(data);
        PricingUnit unit = PricingUnit.fromKey(data.getOrDefault(PricingCallbackHandler.UNIT_KEY, "")).orElse(null);
        if (service.isEmpty() || unit == null) {
            conversations.clear(from.id());
            messenger.sendText(chatId, "این خدمت دیگه در دسترس نیست. دوباره از اول انتخابش کن.");
            messenger.send(chatId, screens.home(from));
            return;
        }

        long amount;
        try {
            amount = PriceAmountParser.parse(raw);
        } catch (InvalidPriceInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }

        try {
            pricing.add(accounts.resolve(from).getId(), service.get(), data.get(PricingCallbackHandler.TITLE_KEY), unit, amount);
            messenger.sendText(chatId, "✅ خدمت اضافه شد.");
        } catch (PriceLimitReachedException e) {
            messenger.sendText(chatId, "منوی قیمتت پره. اول یکی از خدمت‌ها رو حذف کن.");
        }
        conversations.clear(from.id());
        messenger.send(chatId, screens.home(from));
    }
}
