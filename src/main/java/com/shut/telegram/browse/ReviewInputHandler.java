package com.shut.telegram.browse;

import com.shut.common.text.PersianDigits;
import com.shut.review.ReviewService;
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

/** Receives the comment a user types after rating a member, and saves it. */
@Component
@TelegramBotEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
class ReviewInputHandler implements UpdateHandler {

    private static final String CLEAR = "-";

    private final ConversationService conversations;
    private final MembershipGate membershipGate;
    private final TelegramAccountResolver accounts;
    private final ReviewService reviews;
    private final ReviewPages pages;
    private final BrowseSession session;
    private final BotMessenger messenger;

    ReviewInputHandler(ConversationService conversations, MembershipGate membershipGate,
                       TelegramAccountResolver accounts, ReviewService reviews,
                       ReviewPages pages, BotMessenger messenger, BrowseSession session) {
        this.conversations = conversations;
        this.membershipGate = membershipGate;
        this.accounts = accounts;
        this.reviews = reviews;
        this.pages = pages;
        this.session = session;
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
                        .filter(state -> state.getStep() == ConversationStep.AWAITING_REVIEW_COMMENT)
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

        long providerId;
        try {
            providerId = Long.parseLong(state.getData().getOrDefault(ReviewCallbackHandler.PROVIDER_KEY, ""));
        } catch (NumberFormatException e) {
            session.resume(from.id());
            return;
        }

        String text = message.text().strip();
        if (text.length() > ReviewService.MAX_COMMENT_LENGTH) {
            messenger.sendText(chatId, "نظرت خیلی بلنده. حداکثر "
                    + PersianDigits.toPersian(ReviewService.MAX_COMMENT_LENGTH)
                    + " حرف بفرست یا برای انصراف /cancel رو بزن.");
            return;
        }

        boolean saved = reviews.comment(providerId, accounts.resolve(from).getId(), CLEAR.equals(text) ? null : text);
        session.resume(from.id());
        messenger.sendText(chatId, saved
                ? (CLEAR.equals(text) ? "✅ نظرت پاک شد." : "✅ نظرت ذخیره شد.")
                : "اول باید امتیاز بدی.");
        messenger.send(chatId, pages.list(from, providerId, 0));
    }
}
