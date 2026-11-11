package com.shut.telegram.project;

import com.shut.project.Award;
import com.shut.project.Bid;
import com.shut.project.BidService;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Handles the buttons for offers: making, changing and withdrawing one, and the owner's reading and choosing. */
@Component
@TelegramBotEnabled
class BidCallbackHandler extends CallbackHandler {

    private final BotMessenger messenger;
    private final ProjectService projects;
    private final BidService bids;
    private final BidSubmission submission;
    private final ProjectPages pages;
    private final ProjectNotifier notifier;
    private final MemberNames names;
    private final TelegramAccountResolver accounts;

    BidCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                       ProjectService projects, BidService bids, BidSubmission submission, ProjectPages pages,
                       ProjectNotifier notifier, MemberNames names, TelegramAccountResolver accounts) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.projects = projects;
        this.bids = bids;
        this.submission = submission;
        this.pages = pages;
        this.notifier = notifier;
        this.names = names;
        this.accounts = accounts;
    }

    @Override
    protected String prefix() {
        return BidCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        switch (context.arg(0)) {
            case "new" -> number(context, 1).ifPresent(id -> begin(context, id));
            case "nonote" -> submit(context);
            case "abandon" -> abandon(context);
            case "wd" -> number(context, 1).ifPresent(id -> askToConfirmWithdraw(context, id));
            case "wdok" -> number(context, 1).ifPresent(id -> withdraw(context, id));
            case "list" -> number(context, 1).ifPresent(id ->
                    show(context, pages.bidList(context.user(), id, number(context, 2).orElse(0L).intValue())));
            case "view" -> number(context, 1).ifPresent(id -> show(context, pages.bidDetail(context.user(), id)));
            case "pick" -> number(context, 1).ifPresent(id -> askToConfirmPick(context, id));
            case "pickok" -> number(context, 1).ifPresent(id -> pick(context, id));
            case "mine" -> show(context, pages.myBids(context.user(), number(context, 1).orElse(0L).intValue()));
            default -> { }
        }
    }

    private void begin(CallbackContext context, long projectId) {
        try {
            show(context, submission.begin(context.user(), projectId));
        } catch (InvalidProjectInputException e) {
            messenger.sendText(context.chatId(), e.getMessage());
        }
    }

    private void submit(CallbackContext context) {
        try {
            show(context, submission.submit(context.user(), null));
            messenger.sendText(context.chatId(), "✅ پیشنهادت ثبت شد.");
        } catch (InvalidProjectInputException e) {
            messenger.sendText(context.chatId(), e.getMessage());
        }
    }

    private void abandon(CallbackContext context) {
        Optional<Long> projectId = submission.abandon(context.user().id());
        show(context, projectId.map(id -> pages.view(context.user(), id)).orElseGet(ProjectScreens::home));
    }

    private void askToConfirmWithdraw(CallbackContext context, long projectId) {
        show(context, projects.find(projectId).map(BidScreens::withdrawConfirm).orElseGet(ProjectScreens::notAvailable));
    }

    private void withdraw(CallbackContext context, long projectId) {
        bids.withdraw(projectId, userId(context));
        show(context, pages.view(context.user(), projectId));
    }

    private void askToConfirmPick(CallbackContext context, long bidId) {
        Optional<Bid> bid = bids.findForOwner(bidId, userId(context));
        Optional<Project> project = bid.flatMap(found -> projects.find(found.getProjectId()));
        if (bid.isEmpty() || project.isEmpty()) {
            show(context, ProjectScreens.notAvailable());
            return;
        }
        show(context, BidScreens.pickConfirm(project.get(), bid.get(), names.of(bid.get().getBidderId())));
    }

    private void pick(CallbackContext context, long bidId) {
        long ownerId = userId(context);
        Optional<Bid> bid = bids.findForOwner(bidId, ownerId);
        Optional<Award> award = bid.flatMap(found -> projects.award(found.getProjectId(), ownerId, bidId));
        if (award.isEmpty()) {
            messenger.sendText(context.chatId(),
                    "نمی‌شه این پیشنهاد رو انتخاب کرد. شاید پروژه قبلاً واگذار یا لغو شده.");
            show(context, bid.map(found -> pages.view(context.user(), found.getProjectId()))
                    .orElseGet(ProjectScreens::notAvailable));
            return;
        }
        long winnerId = award.get().winner().getBidderId();
        show(context, BidScreens.awarded(award.get().project(), award.get().winner(),
                names.of(winnerId), names.usernameOf(winnerId)));
        notifier.awarded(award.get());
    }

    private long userId(CallbackContext context) {
        return accounts.resolve(context.user()).getId();
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }

    private static Optional<Long> number(CallbackContext context, int index) {
        try {
            return Optional.of(Long.parseLong(context.arg(index)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
