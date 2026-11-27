package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentBrand;
import com.shut.equipment.EquipmentCatalog;
import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentType;
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
import com.shut.user.entity.User;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Receives a type, brand or model name typed because it was not in the list. A name that is new is added to the
 * reference list; one that is already there under another spelling is used as it is.
 */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class EquipmentInputHandler implements UpdateHandler {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_MODEL_LENGTH = 120;
    private static final Set<ConversationStep> STEPS = Set.of(
            ConversationStep.AWAITING_EQUIPMENT_TYPE,
            ConversationStep.AWAITING_EQUIPMENT_BRAND,
            ConversationStep.AWAITING_EQUIPMENT_MODEL);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final EquipmentCatalog catalog;
    private final Map<String, EquipmentChoice> choices;
    private final BotMessenger messenger;

    EquipmentInputHandler(ConversationService conversations, MembershipGate membershipGate,
                          TelegramAccountResolver accounts, EquipmentCatalog catalog,
                          List<EquipmentChoice> choices, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.catalog = catalog;
        this.choices = choices.stream().collect(Collectors.toMap(EquipmentChoice::purpose, Function.identity()));
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
        EquipmentChoice choice = choices.get(state.getData().get(EquipmentCallbackHandler.PURPOSE_KEY));
        if (choice == null) {
            finishGone(from, chatId);
            return;
        }
        User member = accounts.resolve(from);
        long userId = member.getId();
        String name = message.text().strip();
        int limit = state.getStep() == ConversationStep.AWAITING_EQUIPMENT_MODEL ? MAX_MODEL_LENGTH : MAX_NAME_LENGTH;
        if (name.length() > limit) {
            messenger.sendText(chatId, "اسم خیلی بلنده. حداکثر " + limit + " حرف بفرست یا /cancel رو بزن.");
            return;
        }
        try {
            switch (state.getStep()) {
                case AWAITING_EQUIPMENT_TYPE -> receiveType(from, chatId, name, userId, choice);
                case AWAITING_EQUIPMENT_BRAND -> receiveBrand(from, chatId, name, userId, state.getData(), choice);
                case AWAITING_EQUIPMENT_MODEL -> receiveModel(from, chatId, name, member, state.getData(), choice);
                default -> { }
            }
        } catch (IllegalArgumentException e) {
            messenger.sendText(chatId, "اسم باید حروف یا عدد داشته باشه. دوباره بفرست یا /cancel رو بزن.");
        }
    }

    private void receiveType(TelegramUser from, long chatId, String name, long userId, EquipmentChoice choice) {
        EquipmentType type = catalog.typeNamed(name, userId).value();
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.chooseBrand(choice.purpose(), type, catalog.brandsFor(type.getId())));
    }

    private void receiveBrand(TelegramUser from, long chatId, String name, long userId, Map<String, String> data,
                              EquipmentChoice choice) {
        Optional<EquipmentType> type = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.TYPE_KEY, ""))
                .flatMap(catalog::type);
        if (type.isEmpty()) {
            finishGone(from, chatId);
            return;
        }
        EquipmentBrand brand = catalog.brandNamed(name, userId).value();
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.chooseModel(choice.purpose(), type.get(), brand,
                catalog.modelsFor(type.get().getId(), brand.getId())));
    }

    private void receiveModel(TelegramUser from, long chatId, String name, User member, Map<String, String> data,
                              EquipmentChoice choice) {
        Optional<Long> typeId = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.TYPE_KEY, ""));
        Optional<Long> brandId = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.BRAND_KEY, ""));
        if (typeId.isEmpty() || brandId.isEmpty()) {
            finishGone(from, chatId);
            return;
        }
        EquipmentModel model = catalog.modelNamed(typeId.get(), brandId.get(), name, member.getId()).value();
        conversations.clear(from.id());
        EquipmentChoice.Outcome outcome = choice.chosen(member, from.id(), model);
        if (outcome.note() != null) {
            messenger.sendText(chatId, outcome.note());
        }
        messenger.send(chatId, outcome.screen());
    }

    private void finishGone(TelegramUser from, long chatId) {
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.gone());
    }
}
