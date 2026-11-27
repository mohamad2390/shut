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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the equipment buttons: the member's own gear, and the picker (type, brand, model) that every feature
 * using the reference list shares. What happens with the product that was picked is up to the feature's
 * {@link EquipmentChoice}.
 */
@Component
@TelegramBotEnabled
class EquipmentCallbackHandler extends CallbackHandler {

    static final String PURPOSE_KEY = "purpose";
    static final String TYPE_KEY = "type";
    static final String BRAND_KEY = "brand";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final EquipmentCatalog catalog;
    private final EquipmentService equipment;
    private final Map<String, EquipmentChoice> choices;

    EquipmentCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                             ConversationService conversations, TelegramAccountResolver accounts,
                             EquipmentCatalog catalog, EquipmentService equipment, List<EquipmentChoice> choices) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.catalog = catalog;
        this.equipment = equipment;
        this.choices = choices.stream().collect(Collectors.toMap(EquipmentChoice::purpose, Function.identity()));
    }

    @Override
    protected String prefix() {
        return EquipmentCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> show(context, home(context));
            case "rm" -> removeItem(context);
            default -> picker(context);
        }
    }

    /** The picker's buttons all carry the purpose as their second part. */
    private void picker(CallbackContext context) {
        EquipmentChoice choice = choices.get(context.arg(1));
        if (choice == null) {
            show(context, EquipmentScreens.gone());
            return;
        }
        String purpose = choice.purpose();
        switch (context.arg(0)) {
            case "add" -> show(context, EquipmentScreens.chooseType(purpose, catalog.types(),
                    choice.backCallback(), choice.backLabel()));
            case "t" -> show(context, brands(purpose, parse(context.arg(2))));
            case "b" -> show(context, models(purpose, parse(context.arg(2)), parse(context.arg(3))));
            case "m" -> pickModel(context, choice);
            case "ot" -> ask(context, ConversationStep.AWAITING_EQUIPMENT_TYPE, Map.of(PURPOSE_KEY, purpose),
                    EquipmentScreens.askType());
            case "ob" -> askBrand(context, purpose);
            case "om" -> askModel(context, purpose);
            default -> { }
        }
    }

    private Screen home(CallbackContext context) {
        conversations.clear(context.user().id());
        return EquipmentScreens.home(equipment.list(accounts.resolve(context.user()).getId()));
    }

    private Screen brands(String purpose, Optional<Long> typeId) {
        return typeId.flatMap(catalog::type)
                .map(type -> EquipmentScreens.chooseBrand(purpose, type, catalog.brandsFor(type.getId())))
                .orElseGet(EquipmentScreens::gone);
    }

    private Screen models(String purpose, Optional<Long> typeId, Optional<Long> brandId) {
        Optional<EquipmentType> type = typeId.flatMap(catalog::type);
        Optional<EquipmentBrand> brand = brandId.flatMap(catalog::brand);
        if (type.isEmpty() || brand.isEmpty()) {
            return EquipmentScreens.gone();
        }
        return EquipmentScreens.chooseModel(purpose, type.get(), brand.get(),
                catalog.modelsFor(type.get().getId(), brand.get().getId()));
    }

    /** A product was picked from the list: the feature that asked decides what happens next. */
    private void pickModel(CallbackContext context, EquipmentChoice choice) {
        Optional<EquipmentModel> model = parse(context.arg(2)).flatMap(catalog::model);
        if (model.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        EquipmentChoice.Outcome outcome = choice.chosen(accounts.resolve(context.user()), context.user().id(), model.get());
        show(context, outcome.screen());
        if (outcome.note() != null) {
            messenger.sendText(context.chatId(), outcome.note());
        }
    }

    private void removeItem(CallbackContext context) {
        parse(context.arg(1)).ifPresent(id -> equipment.remove(accounts.resolve(context.user()).getId(), id));
        show(context, home(context));
    }

    private void askBrand(CallbackContext context, String purpose) {
        Optional<EquipmentType> type = parse(context.arg(2)).flatMap(catalog::type);
        if (type.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        ask(context, ConversationStep.AWAITING_EQUIPMENT_BRAND,
                Map.of(PURPOSE_KEY, purpose, TYPE_KEY, String.valueOf(type.get().getId())),
                EquipmentScreens.askBrand(type.get()));
    }

    private void askModel(CallbackContext context, String purpose) {
        Optional<EquipmentType> type = parse(context.arg(2)).flatMap(catalog::type);
        Optional<EquipmentBrand> brand = parse(context.arg(3)).flatMap(catalog::brand);
        if (type.isEmpty() || brand.isEmpty()) {
            show(context, EquipmentScreens.gone());
            return;
        }
        ask(context, ConversationStep.AWAITING_EQUIPMENT_MODEL,
                Map.of(PURPOSE_KEY, purpose, TYPE_KEY, String.valueOf(type.get().getId()),
                        BRAND_KEY, String.valueOf(brand.get().getId())),
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
