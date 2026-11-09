package com.shut.telegram.pricing;

import com.shut.pricing.PriceLimitReachedException;
import com.shut.pricing.PricingService;
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
import java.util.Set;

/** Receives the text a user types while adding a service: first its name, then its price. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class PricingInputHandler implements UpdateHandler {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final Set<ConversationStep> PRICING_STEPS = Set.of(
            ConversationStep.AWAITING_PRICE_TITLE,
            ConversationStep.AWAITING_PRICE_UNIT,
            ConversationStep.AWAITING_PRICE_AMOUNT);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final PricingService pricing;
    private final PricingScreenProvider screens;
    private final BotMessenger messenger;

    PricingInputHandler(ConversationService conversations, MembershipGate membershipGate,
                        TelegramAccountResolver accounts, PricingService pricing,
                        PricingScreenProvider screens, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.pricing = pricing;
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
            case AWAITING_PRICE_TITLE -> receiveTitle(from, chatId, message.text());
            case AWAITING_PRICE_UNIT -> messenger.send(chatId,
                    PricingScreens.askUnit(state.getData().getOrDefault(PricingCallbackHandler.TITLE_KEY, "")));
            case AWAITING_PRICE_AMOUNT -> receiveAmount(from, chatId, message.text(), state.getData());
            default -> { }
        }
    }

    private void receiveTitle(TelegramUser from, long chatId, String raw) {
        String title = raw.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            messenger.sendText(chatId, "اسم خدمت خیلی بلنده. حداکثر ۱۰۰ حرف بفرست.");
            return;
        }
        conversations.begin(from.id(), ConversationStep.AWAITING_PRICE_UNIT,
                Map.of(PricingCallbackHandler.TITLE_KEY, title));
        messenger.send(chatId, PricingScreens.askUnit(title));
    }

    private void receiveAmount(TelegramUser from, long chatId, String raw, Map<String, String> data) {
        String title = data.get(PricingCallbackHandler.TITLE_KEY);
        PricingUnit unit = PricingUnit.fromKey(data.getOrDefault(PricingCallbackHandler.UNIT_KEY, "")).orElse(null);
        if (title == null || unit == null) {
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

        try {
            pricing.add(accounts.resolve(from).getId(), title, unit, amount);
            messenger.sendText(chatId, "✅ خدمت اضافه شد.");
        } catch (PriceLimitReachedException e) {
            messenger.sendText(chatId, "منوی قیمتت پره. اول یکی از خدمت‌ها رو حذف کن.");
        }
        conversations.clear(from.id());
        messenger.send(chatId, screens.home(from));
    }
}
