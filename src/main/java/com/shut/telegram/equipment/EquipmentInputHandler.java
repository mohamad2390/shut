package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentBrand;
import com.shut.equipment.EquipmentCatalog;
import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentService;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final EquipmentService equipment;
    private final BotMessenger messenger;

    EquipmentInputHandler(ConversationService conversations, MembershipGate membershipGate,
                          TelegramAccountResolver accounts, EquipmentCatalog catalog,
                          EquipmentService equipment, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.catalog = catalog;
        this.equipment = equipment;
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
        long userId = accounts.resolve(from).getId();
        String name = message.text().strip();
        int limit = state.getStep() == ConversationStep.AWAITING_EQUIPMENT_MODEL ? MAX_MODEL_LENGTH : MAX_NAME_LENGTH;
        if (name.length() > limit) {
            messenger.sendText(chatId, "اسم خیلی بلنده. حداکثر " + limit + " حرف بفرست یا /cancel رو بزن.");
            return;
        }
        try {
            switch (state.getStep()) {
                case AWAITING_EQUIPMENT_TYPE -> receiveType(from, chatId, name, userId);
                case AWAITING_EQUIPMENT_BRAND -> receiveBrand(from, chatId, name, userId, state.getData());
                case AWAITING_EQUIPMENT_MODEL -> receiveModel(from, chatId, name, userId, state.getData());
                default -> { }
            }
        } catch (IllegalArgumentException e) {
            messenger.sendText(chatId, "اسم باید حروف یا عدد داشته باشه. دوباره بفرست یا /cancel رو بزن.");
        }
    }

    private void receiveType(TelegramUser from, long chatId, String name, long userId) {
        EquipmentType type = catalog.typeNamed(name, userId).value();
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.chooseBrand(type, catalog.brandsFor(type.getId())));
    }

    private void receiveBrand(TelegramUser from, long chatId, String name, long userId, Map<String, String> data) {
        Optional<EquipmentType> type = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.TYPE_KEY, ""))
                .flatMap(catalog::type);
        if (type.isEmpty()) {
            finishGone(from, chatId);
            return;
        }
        EquipmentBrand brand = catalog.brandNamed(name, userId).value();
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.chooseModel(type.get(), brand,
                catalog.modelsFor(type.get().getId(), brand.getId())));
    }

    private void receiveModel(TelegramUser from, long chatId, String name, long userId, Map<String, String> data) {
        Optional<Long> typeId = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.TYPE_KEY, ""));
        Optional<Long> brandId = EquipmentCallbackHandler.parse(data.getOrDefault(EquipmentCallbackHandler.BRAND_KEY, ""));
        if (typeId.isEmpty() || brandId.isEmpty()) {
            finishGone(from, chatId);
            return;
        }
        EquipmentModel model = catalog.modelNamed(typeId.get(), brandId.get(), name, userId).value();
        EquipmentService.AddResult result = equipment.add(userId, model.getId());
        conversations.clear(from.id());
        messenger.sendText(chatId, switch (result) {
            case ADDED -> "✅ " + model.displayName() + " به تجهیزاتت اضافه شد.";
            case ALREADY_OWNED -> "این تجهیز از قبل توی فهرستت هست.";
            case LIMIT_REACHED -> "حداکثر " + EquipmentService.MAX_ITEMS + " تجهیز می‌تونی داشته باشی. اول یکی رو حذف کن.";
        });
        messenger.send(chatId, EquipmentScreens.home(equipment.list(userId)));
    }

    private void finishGone(TelegramUser from, long chatId) {
        conversations.clear(from.id());
        messenger.send(chatId, EquipmentScreens.gone());
    }
}
