package com.shut.telegram.business;

import com.shut.business.BusinessKind;
import com.shut.business.BusinessProfile;
import com.shut.business.BusinessService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Receives the typed answers of the studio and rental forms: name, then city, then description. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class BusinessInputHandler implements UpdateHandler {

    static final int MAX_NAME = 120;
    static final int MAX_CITY = 100;
    static final int MAX_DESCRIPTION = 2000;
    private static final String CITY_KEY = "city";
    private static final String NAME_KEY = "name";
    private static final Set<ConversationStep> STEPS = Set.of(ConversationStep.AWAITING_BUSINESS_NAME,
            ConversationStep.AWAITING_BUSINESS_CITY, ConversationStep.AWAITING_BUSINESS_DESCRIPTION);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final BusinessService business;
    private final BotMessenger messenger;

    BusinessInputHandler(ConversationService conversations, MembershipGate membershipGate,
                         TelegramAccountResolver accounts, BusinessService business, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.business = business;
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
        Optional<BusinessKind> kind = state == null ? Optional.empty()
                : BusinessKind.fromKey(state.getData().get(BusinessCallbackHandler.KIND_KEY));
        if (kind.isEmpty()) {
            conversations.clear(from.id());
            return;
        }
        String text = message.text().strip().replaceAll("\\s+", " ");
        switch (state.getStep()) {
            case AWAITING_BUSINESS_NAME -> receiveName(from, chatId, kind.get(), text, state.getData());
            case AWAITING_BUSINESS_CITY -> receiveCity(from, chatId, kind.get(), text, state.getData());
            case AWAITING_BUSINESS_DESCRIPTION -> receiveDescription(from, chatId, kind.get(), message.text().strip(), state.getData());
            default -> { }
        }
    }

    private void receiveName(TelegramUser from, long chatId, BusinessKind kind, String text, Map<String, String> data) {
        if (text.isEmpty() || text.length() > MAX_NAME) {
            messenger.sendText(chatId, "اسم باید بین ۱ تا ۱۲۰ حرف باشه. دوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(NAME_KEY, text);
        conversations.begin(from.id(), ConversationStep.AWAITING_BUSINESS_CITY, next);
        messenger.send(chatId, BusinessScreens.askCity());
    }

    private void receiveCity(TelegramUser from, long chatId, BusinessKind kind, String text, Map<String, String> data) {
        if (text.length() > MAX_CITY) {
            messenger.sendText(chatId, "اسم شهر خیلی بلنده. دوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        if (!skipped(text)) {
            next.put(CITY_KEY, text);
        }
        conversations.begin(from.id(), ConversationStep.AWAITING_BUSINESS_DESCRIPTION, next);
        messenger.send(chatId, BusinessScreens.askDescription());
    }

    private void receiveDescription(TelegramUser from, long chatId, BusinessKind kind, String text,
                                    Map<String, String> data) {
        if (text.length() > MAX_DESCRIPTION) {
            messenger.sendText(chatId, "توضیحات خیلی بلنده؛ حداکثر ۲۰۰۰ حرف. دوباره بفرست یا /cancel رو بزن.");
            return;
        }
        BusinessProfile profile = business.register(accounts.resolve(from), kind, data.get(NAME_KEY),
                data.get(CITY_KEY), skipped(text) ? null : text);
        conversations.clear(from.id());
        messenger.send(chatId, BusinessScreens.done(kind, profile));
    }

    private static boolean skipped(String text) {
        return text.equals("-") || text.isEmpty();
    }
}
