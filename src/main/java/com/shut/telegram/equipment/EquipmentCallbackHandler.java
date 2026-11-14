package com.shut.telegram.equipment;

import com.shut.equipment.EquipmentBrand;
import com.shut.equipment.EquipmentCatalog;
import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentService;
import com.shut.equipment.EquipmentType;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** Handles the equipment buttons: the member's own gear, and picking a type, brand and model to add. */
@Component
@TelegramBotEnabled
class EquipmentCallbackHandler extends CallbackHandler {

    static final String TYPE_KEY = "type";
    static final String BRAND_KEY = "brand";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final EquipmentCatalog catalog;
    private final EquipmentService equipment;

    EquipmentCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                             ConversationService conversations, TelegramAccountResolver accounts,
                             EquipmentCatalog catalog, EquipmentService equipment) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.catalog = catalog;
        this.equipment = equipment;
    }

    @Override
    protected String prefix() {
        return EquipmentCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> show(context, home(context));
            case "add" -> show(context, EquipmentScreens.chooseType(catalog.types()));
            case "t" -> show(context, brands(parse(context.arg(1))));
            case "b" -> show(context, models(parse(context.arg(1)), parse(context.arg(2))));
            case "m" -> addModel(context);
            case "rm" -> removeItem(context);
            case "ot" -> ask(context, ConversationStep.AWAITING_EQUIPMENT_TYPE, Map.of(), EquipmentScreens.askType());
            case "ob" -> askBrand(context);
            case "om" -> askModel(context);
            default -> { }
        }
    }

    private Screen home(CallbackContext context) {
        conversations.clear(context.user().id());
        return EquipmentScreens.home(equipment.list(accounts.resolve(context.user()).getId()));
    }

    private Screen brands(Optional<Long> typeId) {
        return typeId.flatMap(catalog::type)
                .map(type -> EquipmentScreens.chooseBrand(type, catalog.brandsFor(type.getId())))
                .orElseGet(EquipmentScreens::gone);
    }

    private Screen models(Optional<Long> typeId, Optional<Long> brandId) {
        Optional<EquipmentType> type = typeId.flatMap(catalog::type);
        Optional<EquipmentBrand> brand = brandId.flatMap(catalog::brand);
        if (type.isEmpty() || brand.isEmpty()) {
            return EquipmentScreens.gone();
        }
        return EquipmentScreens.chooseModel(type.get(), brand.get(),
                catalog.modelsFor(type.get().getId(), brand.get().getId()));
    }

    private void addModel(CallbackContext context) {
        Optional<EquipmentModel> model = parse(context.arg(1)).flatMap(catalog::model);
        if (model.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        long userId = accounts.resolve(context.user()).getId();
        EquipmentService.AddResult result = equipment.add(userId, model.get().getId());
        messenger.show(context.chatId(), context.messageId(), EquipmentScreens.home(equipment.list(userId)));
        messenger.sendText(context.chatId(), switch (result) {
            case ADDED -> "✅ " + model.get().displayName() + " به تجهیزاتت اضافه شد.";
            case ALREADY_OWNED -> "این تجهیز از قبل توی فهرستت هست.";
            case LIMIT_REACHED -> "حداکثر " + EquipmentService.MAX_ITEMS + " تجهیز می‌تونی داشته باشی. اول یکی رو حذف کن.";
        });
    }

    private void removeItem(CallbackContext context) {
        parse(context.arg(1)).ifPresent(id -> equipment.remove(accounts.resolve(context.user()).getId(), id));
        show(context, home(context));
    }

    private void askBrand(CallbackContext context) {
        Optional<EquipmentType> type = parse(context.arg(1)).flatMap(catalog::type);
        if (type.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        ask(context, ConversationStep.AWAITING_EQUIPMENT_BRAND,
                Map.of(TYPE_KEY, String.valueOf(type.get().getId())), EquipmentScreens.askBrand(type.get()));
    }

    private void askModel(CallbackContext context) {
        Optional<EquipmentType> type = parse(context.arg(1)).flatMap(catalog::type);
        Optional<EquipmentBrand> brand = parse(context.arg(2)).flatMap(catalog::brand);
        if (type.isEmpty() || brand.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        ask(context, ConversationStep.AWAITING_EQUIPMENT_MODEL,
                Map.of(TYPE_KEY, String.valueOf(type.get().getId()), BRAND_KEY, String.valueOf(brand.get().getId())),
                EquipmentScreens.askModel(type.get(), brand.get()));
    }

    private void ask(CallbackContext context, ConversationStep step, Map<String, String> data, Screen prompt) {
        conversations.begin(context.user().id(), step, data);
        show(context, prompt);
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }

    static Optional<Long> parse(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
