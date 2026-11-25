package com.shut.telegram.studio;

import com.shut.studio.Studio;
import com.shut.studio.StudioMember;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Tells a specialist about an invitation and the owner about the answer. A failed message is only logged. */
@Component
@TelegramBotEnabled
class StudioNotifier {

    private static final Logger log = LoggerFactory.getLogger(StudioNotifier.class);

    private final UserService users;
    private final BotMessenger messenger;

    StudioNotifier(UserService users, BotMessenger messenger) {
        this.users = users;
        this.messenger = messenger;
    }

    /** The name anyone may see, without contact details. */
    String displayName(Long userId) {
        User user = users.findById(userId);
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? "یک متخصص" : name;
    }

    /** The name with the Telegram username (or phone), for the studio owner to recognise the person. */
    String nameOf(Long userId) {
        User user = users.findById(userId);
        String name = displayName(userId);
        if (user.getTelegramUsername() != null) {
            return name + " (@" + user.getTelegramUsername() + ")";
        }
        return user.getPhoneNumber() == null ? name : name + " (" + user.getPhoneNumber() + ")";
    }

    /** Sends the specialist the invitation with buttons to accept or decline. False if it could not be sent. */
    boolean invited(Studio studio, StudioMember member) {
        return send(member.getUserId(), new Screen("🏢 دعوت به استودیو\n\n" + nameOf(studio.getOwnerId())
                + " تو رو به لیست افراد استودیوی «" + studio.getName() + "» دعوت کرده. "
                + "اگه قبول کنی، اسمت توی صفحهٔ استودیو برای همه نمایش داده می‌شه.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("✅ قبول", StudioCallbacks.accept(member.getId())),
                                InlineKeyboardButton.callback("❌ رد", StudioCallbacks.decline(member.getId())))
                        .build()));
    }

    /** Tells the owner whether the specialist accepted. */
    void answered(Studio studio, Long userId, boolean accepted) {
        send(studio.getOwnerId(), Screen.of((accepted ? "✅ " : "❌ ") + nameOf(userId)
                + (accepted ? " دعوتت رو قبول کرد و به افراد استودیو اضافه شد." : " دعوتت رو رد کرد.")));
    }

    private boolean send(Long userId, Screen screen) {
        try {
            Long telegramId = users.findById(userId).getTelegramId();
            if (telegramId == null) {
                return false; // a web-only member has no chat to send to
            }
            messenger.send(telegramId, screen);
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not notify user {} about a studio invitation: {}", userId, e.getMessage());
            return false;
        }
    }
}
