package com.shut.telegram.studio;

import com.shut.studio.Studio;
import com.shut.studio.StudioMedia;
import com.shut.studio.StudioMemberService;
import com.shut.studio.StudioService;
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
import com.shut.telegram.storage.MediaBackupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handles the studio owner's buttons: registering, details, services, showcase and people. */
@Component
@TelegramBotEnabled
class StudioOwnerCallbackHandler extends CallbackHandler {

    static final String FIELD_KEY = "field";
    static final String STAGE_KEY = "stage";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final StudioService studios;
    private final StudioMemberService members;
    private final StudioNotifier notifier;
    private final MediaBackupService backups;

    StudioOwnerCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                               ConversationService conversations, TelegramAccountResolver accounts,
                               StudioService studios, StudioMemberService members, StudioNotifier notifier,
                               MediaBackupService backups) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.studios = studios;
        this.members = members;
        this.notifier = notifier;
        this.backups = backups;
    }

    @Override
    protected String prefix() {
        return StudioOwnerCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        conversations.clear(context.user().id());
        Long userId = accounts.resolve(context.user()).getId();
        switch (context.arg(0)) {
            case "mine" -> show(context, studios.ownedBy(userId).map(this::manage)
                    .orElseGet(StudioOwnerScreens::intro));
            case "new" -> show(context, studios.ownedBy(userId).map(this::manage).orElseGet(() -> {
                conversations.begin(context.user().id(), ConversationStep.AWAITING_STUDIO_WIZARD,
                        Map.of(FIELD_KEY, StudioField.NAME.name()));
                return StudioOwnerScreens.ask(StudioField.NAME, true, null);
            }));
            default -> withStudio(context, userId);
        }
    }

    /** Everything else needs the member's studio. */
    private void withStudio(CallbackContext context, Long userId) {
        Optional<Studio> found = studios.ownedBy(userId);
        if (found.isEmpty()) {
            show(context, StudioOwnerScreens.intro());
            return;
        }
        Studio studio = found.get();
        long telegramId = context.user().id();
        Screen screen = switch (context.arg(0)) {
            case "e" -> StudioField.fromKey(context.arg(1)).map(field -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_STUDIO_FIELD, Map.of(FIELD_KEY, field.name()));
                return StudioOwnerScreens.ask(field, false, null);
            }).orElseGet(() -> manage(studio));
            case "tg" -> manage(studios.update(userId, s -> s.setActive(!s.isActive())).orElse(studio));
            case "of" -> StudioOwnerScreens.offerings(studios.offerings(studio.getId()));
            case "oa" -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_OFFERING, Map.of(STAGE_KEY, "TITLE"));
                yield StudioOwnerScreens.askOfferingTitle();
            }
            case "orm" -> {
                parse(context.arg(1)).ifPresent(id -> studios.removeOffering(userId, id));
                yield StudioOwnerScreens.offerings(studios.offerings(studio.getId()));
            }
            case "md" -> StudioOwnerScreens.media(studios.mediaCount(studio.getId()));
            case "ma" -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_STUDIO_MEDIA, Map.of());
                yield StudioOwnerScreens.addMedia(studios.mediaCount(studio.getId()));
            }
            case "mc" -> {
                removeBackups(studios.clearMedia(userId));
                yield StudioOwnerScreens.media(0);
            }
            case "pp" -> people(studio);
            case "iv" -> {
                conversations.begin(telegramId, ConversationStep.AWAITING_STUDIO_INVITEE, Map.of());
                yield StudioOwnerScreens.askInvitee();
            }
            case "prm" -> {
                parse(context.arg(1)).ifPresent(id -> members.remove(studio, id));
                yield people(studio);
            }
            case "del" -> StudioOwnerScreens.confirmDelete(studio);
            case "delok" -> {
                studios.delete(userId).ifPresent(this::removeBackups);
                yield StudioOwnerScreens.intro();
            }
            default -> null;
        };
        if (screen != null) {
            show(context, screen);
        }
    }

    private Screen manage(Studio studio) {
        Long id = studio.getId();
        return StudioOwnerScreens.manage(studio, studios.isListed(studio), studios.offeringCount(id),
                studios.mediaCount(id), members.peopleCount(id), members.pending(id).size());
    }

    private Screen people(Studio studio) {
        return StudioOwnerScreens.people(
                members.people(studio.getId()).stream()
                        .map(member -> new PersonLine(member.getUserId(), notifier.nameOf(member.getUserId()))).toList(),
                members.pending(studio.getId()).stream()
                        .map(member -> new PersonLine(member.getUserId(), notifier.nameOf(member.getUserId()))).toList());
    }

    private void removeBackups(List<StudioMedia> media) {
        media.forEach(item -> backups.remove(item.getBackupMessageId()));
    }

    private static Optional<Long> parse(String raw) {
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
