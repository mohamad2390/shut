package com.shut.telegram.studio;

import com.shut.studio.Studio;
import com.shut.studio.StudioMedia;
import com.shut.studio.StudioMember;
import com.shut.studio.StudioMemberService;
import com.shut.studio.StudioService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.client.TelegramApiException;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.telegram.model.SendMediaRequest;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Handles what anyone does with studios: browsing them, and a specialist answering an invitation. */
@Component
@TelegramBotEnabled
class StudioCallbackHandler extends CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(StudioCallbackHandler.class);
    private static final int PAGE_SIZE = 8;

    private final TelegramApiClient telegram;
    private final BotMessenger messenger;
    private final TelegramAccountResolver accounts;
    private final StudioService studios;
    private final StudioMemberService members;
    private final StudioNotifier notifier;
    private final UserService users;

    StudioCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                          TelegramAccountResolver accounts, StudioService studios, StudioMemberService members,
                          StudioNotifier notifier, UserService users) {
        super(telegram, membershipGate);
        this.telegram = telegram;
        this.messenger = messenger;
        this.accounts = accounts;
        this.studios = studios;
        this.members = members;
        this.notifier = notifier;
        this.users = users;
    }

    @Override
    protected String prefix() {
        return StudioCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "home" -> show(context, StudioScreens.home());
            case "list" -> show(context, StudioScreens.list(studios.listed(page(context.arg(1)), PAGE_SIZE)));
            case "s" -> show(context, listed(context).map(studio -> page(context, studio)).orElseGet(StudioScreens::gone));
            case "of" -> show(context, listed(context).map(studio ->
                    StudioScreens.offerings(studio, studios.offerings(studio.getId()))).orElseGet(StudioScreens::gone));
            case "pp" -> show(context, listed(context).map(this::peopleScreen).orElseGet(StudioScreens::gone));
            case "md" -> sendMedia(context);
            case "lv" -> leave(context);
            case "ia" -> answer(context, true);
            case "id" -> answer(context, false);
            default -> { }
        }
    }

    private Optional<Studio> listed(CallbackContext context) {
        try {
            return studios.findListed(Long.parseLong(context.arg(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Screen page(CallbackContext context, Studio studio) {
        return page(studio, accounts.resolve(context.user()).getId());
    }

    private Screen page(Studio studio, Long viewerId) {
        Long id = studio.getId();
        return StudioScreens.studio(studio, studios.offeringCount(id), studios.mediaCount(id), members.peopleCount(id),
                users.findById(studio.getOwnerId()).getTelegramUsername(), members.isIn(id, viewerId));
    }

    private Screen peopleScreen(Studio studio) {
        List<PersonLine> people = members.people(studio.getId()).stream()
                .map(member -> new PersonLine(member.getUserId(), notifier.displayName(member.getUserId()))).toList();
        return StudioScreens.people(studio, people);
    }

    /** The showcase goes out one by one, then the studio page again so the buttons are at the bottom. */
    private void sendMedia(CallbackContext context) {
        Optional<Studio> studio = listed(context);
        if (studio.isEmpty()) {
            show(context, StudioScreens.gone());
            return;
        }
        List<StudioMedia> media = studios.media(studio.get().getId());
        if (media.isEmpty()) {
            messenger.sendText(context.chatId(), "این استودیو هنوز نمونه‌کاری نذاشته.");
            return;
        }
        media.forEach(item -> sendItem(context.chatId(), item));
        messenger.send(context.chatId(), page(context, studio.get()));
    }

    /** One broken file must not stop the rest from being sent. */
    private void sendItem(long chatId, StudioMedia item) {
        try {
            telegram.sendMedia(item.getKind() == com.shut.studio.MediaKind.VIDEO
                    ? SendMediaRequest.video(chatId, item.getFileId(), null, null)
                    : SendMediaRequest.photo(chatId, item.getFileId(), null, null));
        } catch (TelegramApiException e) {
            log.warn("Could not send studio media {}: {}", item.getId(), e.getMessage());
        }
    }

    private void leave(CallbackContext context) {
        Long userId = accounts.resolve(context.user()).getId();
        Optional<Studio> studio = listed(context);
        studio.ifPresent(found -> members.leave(found.getId(), userId));
        show(context, studio.map(found -> page(found, userId)).orElseGet(StudioScreens::gone));
    }

    /** The specialist's answer to an invitation: tell the owner and show the specialist what happened. */
    private void answer(CallbackContext context, boolean accept) {
        Long userId = accounts.resolve(context.user()).getId();
        Optional<StudioMember> answered;
        try {
            answered = members.respond(Long.parseLong(context.arg(1)), userId, accept);
        } catch (NumberFormatException e) {
            answered = Optional.empty();
        }
        Optional<Studio> studio = answered.flatMap(member -> studios.find(member.getStudioId()));
        if (studio.isEmpty()) {
            show(context, Screen.of("این دعوت دیگه معتبر نیست."));
            return;
        }
        notifier.answered(studio.get(), userId, accept);
        if (!accept) {
            show(context, Screen.of("دعوت رد شد."));
            return;
        }
        show(context, new Screen("✅ به افراد استودیوی «" + studio.get().getName() + "» اضافه شدی.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("🏢 صفحهٔ استودیو", StudioCallbacks.studio(studio.get().getId())))
                        .build()));
    }

    private static int page(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }
}
