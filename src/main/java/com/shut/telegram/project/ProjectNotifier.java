package com.shut.telegram.project;

import com.shut.common.text.PersianDigits;
import com.shut.project.Award;
import com.shut.project.Bid;
import com.shut.project.PlacedBid;
import com.shut.project.Project;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.KeyboardBuilder;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.InlineKeyboardButton;
import com.shut.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tells members what happened to their projects and offers. A message that cannot be delivered, for example because
 * the member blocked the bot, is logged and skipped: it must never undo what the member just did.
 */
@Component
@TelegramBotEnabled
class ProjectNotifier {

    private static final Logger log = LoggerFactory.getLogger(ProjectNotifier.class);

    private final BotMessenger messenger;
    private final UserService users;
    private final MemberNames names;

    ProjectNotifier(BotMessenger messenger, UserService users, MemberNames names) {
        this.messenger = messenger;
        this.users = users;
        this.names = names;
    }

    /** Tells the project's owner about a new or changed offer. */
    void newBid(PlacedBid placed) {
        Project project = placed.project();
        Bid bid = placed.bid();
        String text = (placed.revised() ? "✏️ یه پیشنهاد عوض شد" : "📥 پیشنهاد تازه")
                + " برای «" + project.getTitle() + "»\n\n"
                + names.of(bid.getBidderId()) + ": " + ProjectText.money(bid.getAmount());
        send(project.getOwnerId(), new Screen(text, new KeyboardBuilder()
                .row(InlineKeyboardButton.callback("📥 دیدن پیشنهادها", BidCallbacks.list(project.getId(), 0)))
                .build()));
    }

    /** Tells the chosen provider they won and everyone else that another offer was chosen. */
    void awarded(Award award) {
        Project project = award.project();
        send(award.winner().getBidderId(), new Screen(
                "🎉 پیشنهادت برای «" + project.getTitle() + "» انتخاب شد!\n\nقیمت: "
                        + ProjectText.money(award.winner().getAmount())
                        + "\nصاحب پروژه (" + names.of(project.getOwnerId()) + ") باهات هماهنگ می‌کنه؛ "
                        + "می‌تونی از صفحه‌ی پروژه هم بهش پیام بدی.",
                new KeyboardBuilder()
                        .row(InlineKeyboardButton.callback("📋 دیدن پروژه", ProjectCallbacks.view(project.getId())))
                        .build()));
        award.rejected().forEach(bid -> send(bid.getBidderId(), Screen.of(
                "برای «" + project.getTitle() + "» پیشنهاد دیگه‌ای انتخاب شد. ممنون که پیشنهاد دادی؛ "
                        + "پروژه‌های باز دیگه هم هستن.")));
    }

    /** Tells the owner their project's deadline has passed and that it is time to choose. */
    void deadlinePassed(Project project, long bidCount) {
        KeyboardBuilder keyboard = new KeyboardBuilder();
        String text;
        if (bidCount > 0) {
            text = "⏰ مهلت پروژه‌ی «" + project.getTitle() + "» تموم شد. "
                    + PersianDigits.toPersian(bidCount) + " پیشنهاد داری؛ وقتشه انتخاب کنی.";
            keyboard.row(InlineKeyboardButton.callback("📥 دیدن پیشنهادها", BidCallbacks.list(project.getId(), 0)));
        } else {
            text = "⏰ مهلت پروژه‌ی «" + project.getTitle() + "» تموم شد و پیشنهادی نرسید. "
                    + "می‌تونی پروژه رو لغو کنی و با مهلت بیشتر دوباره تعریفش کنی.";
            keyboard.row(InlineKeyboardButton.callback("📋 دیدن پروژه", ProjectCallbacks.view(project.getId())));
        }
        send(project.getOwnerId(), new Screen(text, keyboard.build()));
    }

    private void send(long userId, Screen screen) {
        try {
            messenger.send(users.findById(userId).getTelegramId(), screen);
        } catch (RuntimeException e) {
            log.warn("Could not notify user {}: {}", userId, e.getMessage());
        }
    }
}
