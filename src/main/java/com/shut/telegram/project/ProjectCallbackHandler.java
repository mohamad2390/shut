package com.shut.telegram.project;

import com.shut.common.text.PersianDigits;
import com.shut.project.Project;
import com.shut.project.ProjectLimitReachedException;
import com.shut.project.ProjectService;
import com.shut.showcase.ShowcaseCategory;
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
import java.util.function.Supplier;

/** Handles the buttons of the projects section: the home page, creating, browsing and cancelling projects. */
@Component
@TelegramBotEnabled
class ProjectCallbackHandler extends CallbackHandler {

    private final BotMessenger messenger;
    private final ProjectService projects;
    private final ProjectCreation creation;
    private final ProjectPages pages;
    private final TelegramAccountResolver accounts;

    ProjectCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                           ProjectService projects, ProjectCreation creation, ProjectPages pages,
                           TelegramAccountResolver accounts) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.projects = projects;
        this.creation = creation;
        this.pages = pages;
        this.accounts = accounts;
    }

    @Override
    protected String prefix() {
        return ProjectCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        long telegramId = context.user().id();
        switch (context.arg(0)) {
            case "home", "abandon" -> {
                creation.abandon(telegramId);
                show(context, ProjectScreens.home());
            }
            case "new" -> show(context, creation.begin(telegramId));
            case "cat" -> ShowcaseCategory.fromKey(context.arg(1)).ifPresent(category ->
                    step(context, () -> creation.chooseCategory(telegramId, category)));
            case "skipcity" -> step(context, () -> creation.receiveCity(telegramId, null));
            case "skipbudget" -> step(context, () -> creation.receiveBudget(telegramId, null));
            case "dl" -> parseNumber(context.arg(1)).ifPresent(days -> createProject(context, days.intValue()));
            case "open" -> show(context, pages.openList(context.user(), context.arg(1),
                    parseNumber(context.arg(2)).orElse(0L).intValue()));
            case "mine" -> show(context, pages.mine(context.user(), parseNumber(context.arg(1)).orElse(0L).intValue()));
            case "view" -> parseNumber(context.arg(1)).ifPresent(id -> show(context, pages.view(context.user(), id)));
            case "cancel" -> parseNumber(context.arg(1)).ifPresent(id -> askToConfirmCancel(context, id));
            case "cancelok" -> parseNumber(context.arg(1)).ifPresent(id -> cancel(context, id));
            default -> { }
        }
    }

    /** Shows the next step's screen, or tells the user why they cannot go on. */
    private void step(CallbackContext context, Supplier<Screen> next) {
        try {
            show(context, next.get());
        } catch (InvalidProjectInputException e) {
            messenger.sendText(context.chatId(), e.getMessage());
        }
    }

    private void createProject(CallbackContext context, int days) {
        try {
            Project project = creation.finish(context.user().id(), accounts.resolve(context.user()).getId(), days);
            show(context, pages.view(context.user(), project.getId()));
            messenger.sendText(context.chatId(), "✅ پروژه ثبت شد. تا مهلتش، عضوهای کانال می‌تونن توی «پروژه‌های باز» ببیننش و پیشنهاد بدن؛ "
                    + "هر پیشنهاد که برسه بهت خبر می‌دم.");
        } catch (ProjectLimitReachedException e) {
            messenger.sendText(context.chatId(), "الان " + PersianDigits.toPersian(ProjectService.MAX_OPEN_PROJECTS) + " پروژه‌ی باز داری که حداکثره. اول یکی رو واگذار یا لغو کن.");
        } catch (InvalidProjectInputException e) {
            messenger.sendText(context.chatId(), e.getMessage());
        }
    }

    private void askToConfirmCancel(CallbackContext context, long projectId) {
        Optional<Project> project = ownedProject(context, projectId);
        show(context, project.map(ProjectScreens::cancelConfirm).orElseGet(ProjectScreens::notAvailable));
    }

    private void cancel(CallbackContext context, long projectId) {
        projects.cancel(projectId, accounts.resolve(context.user()).getId());
        show(context, pages.view(context.user(), projectId));
    }

    private Optional<Project> ownedProject(CallbackContext context, long projectId) {
        long userId = accounts.resolve(context.user()).getId();
        return projects.find(projectId).filter(project -> project.getOwnerId().equals(userId));
    }

    private void show(CallbackContext context, Screen screen) {
        messenger.show(context.chatId(), context.messageId(), screen);
    }

    private static Optional<Long> parseNumber(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
