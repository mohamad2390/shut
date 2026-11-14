package com.shut.telegram.widget;

import com.shut.browse.ProviderDetails;
import com.shut.showcase.ShowcaseCategory;
import com.shut.showcase.ShowcaseService;
import com.shut.telegram.browse.ShowcaseSender;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.stereotype.Component;

/**
 * The showcase widget lists the categories. A media file cannot be drawn inside a widget, so a tap on a category
 * ({@code "o:PHOTOGRAPHY"}) sends the first photos or videos of it to the chat, with the button for the next ones.
 */
@Component
@TelegramBotEnabled
class ShowcaseWidgetAnswerer implements WidgetAnswerer {

    private static final String OPEN_PREFIX = "o:";

    private final ShowcaseService showcase;
    private final ShowcaseSender sender;

    ShowcaseWidgetAnswerer(ShowcaseService showcase, ShowcaseSender sender) {
        this.showcase = showcase;
        this.sender = sender;
    }

    @Override
    public WidgetKind kind() {
        return WidgetKind.SHOWCASE;
    }

    @Override
    public String answer(ProviderDetails provider, String request) {
        return ShowcaseWidgetState.of(WidgetNames.of(provider), showcase.countsByCategory(provider.userId()));
    }

    @Override
    public boolean open(long chatId, ProviderDetails provider, String request) {
        if (request == null || !request.startsWith(OPEN_PREFIX)) {
            return false;
        }
        ShowcaseCategory.fromKey(request.substring(OPEN_PREFIX.length()))
                .ifPresent(category -> sender.sendPage(chatId, provider.userId(), category, 0));
        return true;
    }
}
