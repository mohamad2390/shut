package com.shut.telegram.studio;

import com.shut.pricing.PricingService;
import com.shut.studio.Studio;
import com.shut.studio.StudioMemberService;
import com.shut.studio.StudioMemberService.Invitation;
import com.shut.studio.StudioService;
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

/** Receives the text typed for a studio: registration answers, one edited detail, a service, or whom to invite. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class StudioInputHandler implements UpdateHandler {

    static final int MAX_TITLE = 120;
    static final int MAX_INCLUDES = 500;
    private static final String TITLE_KEY = "title";
    private static final String INCLUDES_KEY = "includes";
    private static final Set<ConversationStep> STEPS = Set.of(ConversationStep.AWAITING_STUDIO_WIZARD,
            ConversationStep.AWAITING_STUDIO_FIELD, ConversationStep.AWAITING_OFFERING,
            ConversationStep.AWAITING_STUDIO_INVITEE);

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final StudioService studios;
    private final StudioMemberService members;
    private final StudioNotifier notifier;
    private final BotMessenger messenger;

    StudioInputHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, StudioService studios, StudioMemberService members,
                       StudioNotifier notifier, BotMessenger messenger) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.studios = studios;
        this.members = members;
        this.notifier = notifier;
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
            case AWAITING_STUDIO_WIZARD -> register(from, chatId, user, message.text(), state.getData());
            case AWAITING_STUDIO_FIELD -> editField(from, chatId, user, message.text(), state.getData());
            case AWAITING_OFFERING -> offering(from, chatId, user, message.text(), state.getData());
            case AWAITING_STUDIO_INVITEE -> invite(from, chatId, user, message.text());
            default -> { }
        }
    }

    /** Each answer is checked and kept in the conversation; the studio is created after the last one. */
    private void register(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        Optional<StudioField> field = StudioField.fromKey(data.get(StudioOwnerCallbackHandler.FIELD_KEY));
        if (field.isEmpty()) {
            conversations.clear(from.id());
            return;
        }
        String value;
        try {
            value = field.get().value(raw);
        } catch (InvalidStudioInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        Map<String, String> next = new HashMap<>(data);
        next.put(field.get().name(), value);
        Optional<StudioField> following = field.get().nextInRegistration();
        if (following.isPresent()) {
            next.put(StudioOwnerCallbackHandler.FIELD_KEY, following.get().name());
            conversations.begin(from.id(), ConversationStep.AWAITING_STUDIO_WIZARD, next);
            messenger.send(chatId, StudioOwnerScreens.ask(following.get(), true, null));
            return;
        }
        conversations.clear(from.id());
        Optional<Studio> created = studios.create(user, next.get(StudioField.NAME.name()));
        if (created.isEmpty()) {
            messenger.sendText(chatId, "قبلاً یک استودیو ثبت کردی.");
            return;
        }
        studios.update(user.getId(), studio -> {
            for (StudioField each : StudioField.REGISTRATION) {
                each.applyTo(studio, next.get(each.name()));
            }
        });
        messenger.sendText(chatId, "✅ استودیو ثبت شد. برای نمایش به همه، حداقل یک خدمت با تعرفه اضافه کن؛ "
                + "نمونه‌کار و افراد استودیو هم اختیاریه.");
        messenger.send(chatId, manage(user));
    }

    private void editField(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        Optional<StudioField> field = StudioField.fromKey(data.get(StudioOwnerCallbackHandler.FIELD_KEY));
        if (field.isEmpty()) {
            conversations.clear(from.id());
            return;
        }
        Optional<Studio> updated;
        try {
            updated = studios.update(user.getId(), studio -> field.get().applyTo(studio, raw));
        } catch (InvalidStudioInputException e) {
            messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        if (updated.isEmpty()) {
            messenger.send(chatId, StudioOwnerScreens.gone());
            return;
        }
        messenger.sendText(chatId, "✅ " + field.get().label() + " ذخیره شد.");
        messenger.send(chatId, manage(user));
    }

    /** A service takes three answers: its title, what it includes, and its price. */
    private void offering(TelegramUser from, long chatId, User user, String raw, Map<String, String> data) {
        String text = raw.strip().replaceAll("[ \\t]+", " ");
        Map<String, String> next = new HashMap<>(data);
        switch (data.getOrDefault(StudioOwnerCallbackHandler.STAGE_KEY, "")) {
            case "TITLE" -> {
                if (text.isEmpty() || text.length() > MAX_TITLE) {
                    messenger.sendText(chatId, "اسم خدمت باید بین ۱ تا ۱۲۰ حرف باشه. دوباره بفرست یا /cancel رو بزن.");
                    return;
                }
                next.put(TITLE_KEY, text);
                next.put(StudioOwnerCallbackHandler.STAGE_KEY, "INCLUDES");
                conversations.begin(from.id(), ConversationStep.AWAITING_OFFERING, next);
                messenger.send(chatId, StudioOwnerScreens.askOfferingIncludes());
            }
            case "INCLUDES" -> {
                if (text.length() > MAX_INCLUDES) {
                    messenger.sendText(chatId, "توضیح خیلی بلنده؛ حداکثر ۵۰۰ حرف. دوباره بفرست یا /cancel رو بزن.");
                    return;
                }
                if (!text.isEmpty() && !text.equals("-")) {
                    next.put(INCLUDES_KEY, text);
                }
                next.put(StudioOwnerCallbackHandler.STAGE_KEY, "PRICE");
                conversations.begin(from.id(), ConversationStep.AWAITING_OFFERING, next);
                messenger.send(chatId, StudioOwnerScreens.askOfferingPrice());
            }
            case "PRICE" -> savePrice(from, chatId, user, text, data);
            default -> conversations.clear(from.id());
        }
    }

    private void savePrice(TelegramUser from, long chatId, User user, String text, Map<String, String> data) {
        Long price = null;
        if (!text.isEmpty() && !text.equals("-")) {
            try {
                long amount = PriceAmountParser.parse(text);
                if (amount <= 0 || amount > PricingService.MAX_AMOUNT) {
                    messenger.sendText(chatId, "قیمت باید بیشتر از صفر باشه. دوباره بفرست یا /cancel رو بزن.");
                    return;
                }
                price = amount;
            } catch (InvalidPriceInputException e) {
                messenger.sendText(chatId, e.getMessage() + "\n\nدوباره بفرست یا /cancel رو بزن.");
                return;
            }
        }
        conversations.clear(from.id());
        if (studios.addOffering(user.getId(), data.get(TITLE_KEY), data.get(INCLUDES_KEY), price).isEmpty()) {
            messenger.sendText(chatId, "نتونستم خدمت رو اضافه کنم؛ شاید به سقف ۱۵ خدمت رسیدی.");
        } else {
            messenger.sendText(chatId, "✅ خدمت اضافه شد.");
        }
        studios.ownedBy(user.getId()).ifPresent(studio ->
                messenger.send(chatId, StudioOwnerScreens.offerings(studios.offerings(studio.getId()))));
    }

    private void invite(TelegramUser from, long chatId, User user, String raw) {
        Optional<Studio> studio = studios.ownedBy(user.getId());
        if (studio.isEmpty()) {
            conversations.clear(from.id());
            messenger.send(chatId, StudioOwnerScreens.gone());
            return;
        }
        Invitation invitation = members.invite(studio.get(), raw);
        String refusal = switch (invitation.result()) {
            case NOT_FOUND -> "کسی با این شماره یا یوزرنیم توی شات پیدا نشد. باید قبلاً توی شات ثبت‌نام کرده باشه.";
            case NOT_SPECIALIST -> "این کاربر هنوز به‌عنوان متخصص ثبت‌نام نکرده.";
            case SELF -> "خودت صاحب استودیویی!";
            case ALREADY_IN -> "این متخصص قبلاً به افراد استودیو اضافه شده.";
            case ALREADY_INVITED -> "قبلاً دعوتش کردی و منتظر جوابشه.";
            case LIMIT_REACHED -> "لیست افراد استودیو پر شده.";
            case INVITED -> null;
        };
        if (refusal != null) {
            messenger.sendText(chatId, refusal + "\n\nشماره یا یوزرنیم دیگه‌ای بفرست یا /cancel رو بزن.");
            return;
        }
        conversations.clear(from.id());
        boolean delivered = notifier.invited(studio.get(), invitation.member());
        messenger.sendText(chatId, "✅ دعوت برای " + notifier.displayName(invitation.user().getId())
                + (delivered ? " فرستاده شد. بعد از قبول کردنش به افراد استودیو اضافه می‌شه."
                : " ثبت شد، ولی پیام تلگرامی بهش نرسید."));
        messenger.send(chatId, StudioOwnerScreens.people(
                members.people(studio.get().getId()).stream()
                        .map(member -> new PersonLine(member.getUserId(), notifier.nameOf(member.getUserId()))).toList(),
                members.pending(studio.get().getId()).stream()
                        .map(member -> new PersonLine(member.getUserId(), notifier.nameOf(member.getUserId()))).toList()));
    }

    private com.shut.telegram.messaging.Screen manage(User user) {
        Studio studio = studios.ownedBy(user.getId()).orElseThrow();
        Long id = studio.getId();
        return StudioOwnerScreens.manage(studio, studios.isListed(studio), studios.offeringCount(id),
                studios.mediaCount(id), members.peopleCount(id), members.pending(id).size());
    }
}
