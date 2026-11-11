package com.shut.telegram.project;

import com.shut.common.text.PersianDigits;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.showcase.ShowcaseCategory;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationState;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.messaging.Screen;
import com.shut.telegram.pricing.InvalidPriceInputException;
import com.shut.telegram.pricing.PriceAmountParser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The steps of defining a project: title, category, description, city, budget and deadline. Each step keeps what
 * was entered so far in the user's conversation state, and returns the screen for the next step.
 */
@Component
@TelegramBotEnabled
class ProjectCreation {

    private static final String TITLE = "title";
    private static final String CATEGORY = "category";
    private static final String DESCRIPTION = "description";
    private static final String CITY = "city";
    private static final String BUDGET = "budget";
    private static final String EXPIRED = "این مرحله دیگه فعال نیست. از «پروژه‌ی جدید» دوباره شروع کن.";

    static final Set<ConversationStep> STEPS = Set.of(
            ConversationStep.AWAITING_PROJECT_TITLE,
            ConversationStep.AWAITING_PROJECT_CATEGORY,
            ConversationStep.AWAITING_PROJECT_DESCRIPTION,
            ConversationStep.AWAITING_PROJECT_CITY,
            ConversationStep.AWAITING_PROJECT_BUDGET,
            ConversationStep.AWAITING_PROJECT_DEADLINE);

    private final ConversationService conversations;
    private final ProjectService projects;

    ProjectCreation(ConversationService conversations, ProjectService projects) {
        this.conversations = conversations;
        this.projects = projects;
    }

    Screen begin(long telegramId) {
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_TITLE, Map.of());
        return ProjectScreens.askTitle();
    }

    /** Leaves the project being created, if the user is creating one. */
    void abandon(long telegramId) {
        if (conversations.find(telegramId).map(ConversationState::getStep).filter(STEPS::contains).isPresent()) {
            conversations.clear(telegramId);
        }
    }

    Screen receiveTitle(long telegramId, String text) {
        String title = text.strip();
        if (title.isEmpty() || title.length() > ProjectService.MAX_TITLE_LENGTH) {
            throw new InvalidProjectInputException("عنوان باید بین ۱ تا " + PersianDigits
                    .toPersian(ProjectService.MAX_TITLE_LENGTH) + " حرف باشه.");
        }
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_CATEGORY, Map.of(TITLE, title));
        return ProjectScreens.askCategory(title);
    }

    Screen chooseCategory(long telegramId, ShowcaseCategory category) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_PROJECT_CATEGORY);
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_DESCRIPTION, with(data, CATEGORY, category.name()));
        return ProjectScreens.askDescription();
    }

    Screen receiveDescription(long telegramId, String text) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_PROJECT_DESCRIPTION);
        String description = text.strip();
        if (description.isEmpty() || description.length() > ProjectService.MAX_DESCRIPTION_LENGTH) {
            throw new InvalidProjectInputException("توضیح باید بین ۱ تا " + PersianDigits
                    .toPersian(ProjectService.MAX_DESCRIPTION_LENGTH) + " حرف باشه.");
        }
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_CITY, with(data, DESCRIPTION, description));
        return ProjectScreens.askCity();
    }

    /** @param text the city, or null if the user said it does not matter */
    Screen receiveCity(long telegramId, String text) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_PROJECT_CITY);
        String city = text == null ? null : text.strip();
        if (city != null && (city.isEmpty() || city.length() > ProjectService.MAX_CITY_LENGTH)) {
            throw new InvalidProjectInputException("اسم شهر باید بین ۱ تا " + PersianDigits
                    .toPersian(ProjectService.MAX_CITY_LENGTH) + " حرف باشه.");
        }
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_BUDGET,
                city == null ? data : with(data, CITY, city));
        return ProjectScreens.askBudget();
    }

    /** @param text the budget in tomans, or null if the user does not want to say */
    Screen receiveBudget(long telegramId, String text) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_PROJECT_BUDGET);
        Map<String, String> next = data;
        if (text != null) {
            try {
                next = with(data, BUDGET, Long.toString(PriceAmountParser.parse(text)));
            } catch (InvalidPriceInputException e) {
                throw new InvalidProjectInputException(e.getMessage());
            }
        }
        conversations.begin(telegramId, ConversationStep.AWAITING_PROJECT_DEADLINE, next);
        return ProjectScreens.askDeadline();
    }

    /** Creates the project from what was collected and ends the conversation. */
    Project finish(long telegramId, long ownerId, int deadlineDays) {
        Map<String, String> data = expect(telegramId, ConversationStep.AWAITING_PROJECT_DEADLINE);
        Optional<ShowcaseCategory> category = ShowcaseCategory.fromKey(data.getOrDefault(CATEGORY, ""));
        if (category.isEmpty() || !data.containsKey(TITLE) || !data.containsKey(DESCRIPTION)) {
            conversations.clear(telegramId);
            throw new InvalidProjectInputException(EXPIRED);
        }
        try {
            Project project = projects.create(ownerId, data.get(TITLE), category.get(), data.get(DESCRIPTION),
                    data.get(CITY), data.containsKey(BUDGET) ? Long.valueOf(data.get(BUDGET)) : null, deadlineDays);
            conversations.clear(telegramId);
            return project;
        } catch (IllegalArgumentException e) {
            conversations.clear(telegramId);
            throw new InvalidProjectInputException(EXPIRED);
        }
    }

    /** The prompt for the step the user is on, for when they type where a button was expected. */
    Optional<Screen> currentPrompt(long telegramId) {
        return conversations.find(telegramId).map(state -> switch (state.getStep()) {
            case AWAITING_PROJECT_CATEGORY -> ProjectScreens.askCategory(state.getData().getOrDefault(TITLE, ""));
            case AWAITING_PROJECT_DEADLINE -> ProjectScreens.askDeadline();
            default -> null;
        });
    }

    private Map<String, String> expect(long telegramId, ConversationStep step) {
        return conversations.find(telegramId)
                .filter(state -> state.getStep() == step)
                .map(ConversationState::getData)
                .orElseThrow(() -> new InvalidProjectInputException(EXPIRED));
    }

    private static Map<String, String> with(Map<String, String> data, String key, String value) {
        Map<String, String> copy = new HashMap<>(data);
        copy.put(key, value);
        return copy;
    }
}
