package com.shut.telegram.project;

import com.shut.browse.ProviderSearchService;
import com.shut.common.text.PersianDigits;
import com.shut.project.BidRefusedException;
import com.shut.project.BidService;
import com.shut.project.PlacedBid;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.model.TelegramUser;
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;
import com.shut.telegram.account.TelegramAccountResolver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The steps of making an offer on a project: the price, then an optional note. */
@Component
@TelegramBotEnabled
class BidSubmission {

    private static final String PROJECT = "projectId";
    private static final String AMOUNT = "amount";
    private static final String EXPIRED = "این مرحله دیگه فعال نیست. دوباره از صفحه‌ی پروژه شروع کن.";

    static final Set<ConversationStep> STEPS = Set.of(
            ConversationStep.AWAITING_BID_AMOUNT, ConversationStep.AWAITING_BID_NOTE);

    private final ConversationService conversations;
    private final ProjectService projects;
    private final BidService bids;
    private final ProviderSearchService providers;
    private final TelegramAccountResolver accounts;
    private final ProjectNotifier notifier;
    private final ProjectPages pages;

    BidSubmission(ConversationService conversations, ProjectService projects, BidService bids,
                  ProviderSearchService providers, TelegramAccountResolver accounts,
                  ProjectNotifier notifier, ProjectPages pages) {
        this.conversations = conversations;
        this.projects = projects;
        this.bids = bids;
        this.providers = providers;
        this.accounts = accounts;
        this.notifier = notifier;
        this.pages = pages;
    }

    /**
     * Starts an offer on the project, or a new price for the user's earlier offer.
     *
     * @throws InvalidProjectInputException if the user cannot bid on this project right now
     */
    Screen begin(TelegramUser from, long projectId) {
        long userId = accounts.resolve(from).getId();
        Project project = projects.find(projectId)
                .orElseThrow(() -> new InvalidProjectInputException("این پروژه دیگه در دسترس نیست."));
        if (project.getOwnerId().equals(userId)) {
            throw new InvalidProjectInputException("نمی‌تونی روی پروژه‌ی خودت پیشنهاد بدی.");
        }
        if (!projects.isAcceptingBids(project)) {
            throw new InvalidProjectInputException("مهلت پیشنهاد دادن برای این پروژه تموم شده.");
        }
        if (providers.find(userId).isEmpty()) {
            throw new InvalidProjectInputException(
                    "برای پیشنهاد دادن، اول توی «👤 پروفایل» اسم نمایشی‌ت رو بنویس تا صاحب پروژه بدونه کی هستی.");
        }
        conversations.begin(from.id(), ConversationStep.AWAITING_BID_AMOUNT, Map.of(PROJECT, Long.toString(projectId)));
        return BidScreens.askAmount(project, bids.findOwn(projectId, userId).orElse(null));
    }

    Screen receiveAmount(long telegramId, String text) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_BID_AMOUNT);
        long amount;
        try {
            amount = PriceAmountParser.parse(text);
        } catch (InvalidPriceInputException e) {
            throw new InvalidProjectInputException(e.getMessage());
        }
        conversations.begin(telegramId, ConversationStep.AWAITING_BID_NOTE,
                Map.of(PROJECT, data.getOrDefault(PROJECT, ""), AMOUNT, Long.toString(amount)));
        return BidScreens.askNote(amount);
    }

    /**
     * Saves the offer, tells the project's owner and returns the project's page.
     *
     * @param note the note, or null for none
     */
    Screen submit(TelegramUser from, String note) {
        Map<String, String> data = expect(from.id(), ConversationStep.AWAITING_BID_NOTE);
        long projectId;
        long amount;
        try {
            projectId = Long.parseLong(data.getOrDefault(PROJECT, ""));
            amount = Long.parseLong(data.getOrDefault(AMOUNT, ""));
        } catch (NumberFormatException e) {
            conversations.clear(from.id());
            throw new InvalidProjectInputException(EXPIRED);
        }

        PlacedBid placed;
        try {
            placed = bids.place(projectId, accounts.resolve(from).getId(), amount, note);
        } catch (BidRefusedException e) {
            conversations.clear(from.id());
            throw new InvalidProjectInputException(switch (e.reason()) {
                case PROJECT_NOT_FOUND -> "این پروژه دیگه در دسترس نیست.";
                case NOT_TAKING_BIDS -> "مهلت پیشنهاد دادن برای این پروژه تموم شد یا واگذار شده.";
                case OWN_PROJECT -> "نمی‌تونی روی پروژه‌ی خودت پیشنهاد بدی.";
            });
        } catch (IllegalArgumentException e) {
            throw new InvalidProjectInputException("توضیح خیلی بلنده. حداکثر "
                    + PersianDigits.toPersian(BidService.MAX_NOTE_LENGTH) + " حرف بفرست.");
        }
        conversations.clear(from.id());
        notifier.newBid(placed);
        return pages.view(from, projectId);
    }

    /** Leaves the offer being written. Returns the project it was for, if there was one. */
    Optional<Long> abandon(long telegramId) {
        Optional<ConversationState> state = conversations.find(telegramId).filter(s -> STEPS.contains(s.getStep()));
        state.ifPresent(s -> conversations.clear(telegramId));
        return state.map(s -> s.getData().get(PROJECT)).flatMap(BidSubmission::parse);
    }

    private Map<String, String> expect(long telegramId, ConversationStep step) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == step)
                .map(ConversationState::getData)
                .orElseThrow(() -> new InvalidProjectInputException(EXPIRED));
    }

    private static Optional<Long> parse(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
