package com.shut.telegram.messaging;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.model.InlineKeyboardButton;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links that open the bot on a member's page: {@code https://t.me/<bot>?start=p<id>}. Whoever taps one lands in the
 * bot with that member's profile open; they must be channel members like everyone else.
 */
@Component
@TelegramBotEnabled
public class BotLinks {

    private static final String PROFILE_PREFIX = "p";
    private static final Pattern PROFILE_PAYLOAD = Pattern.compile("p(\\d{1,18})");

    private final TelegramApiClient telegram;
    private volatile String botUsername;

    public BotLinks(TelegramApiClient telegram) {
        this.telegram = telegram;
    }

    /** The link to a member's profile, or empty if the bot's username cannot be found right now. */
    public Optional<String> profileLink(long userId) {
        String username = username();
        return username == null ? Optional.empty()
                : Optional.of("https://t.me/" + username + "?start=" + PROFILE_PREFIX + userId);
    }

    /** The link that opens the bot, or empty if its username cannot be found right now. */
    public Optional<String> botUrl() {
        String username = username();
        return username == null ? Optional.empty() : Optional.of("https://t.me/" + username);
    }

    /** A button that opens Telegram's share sheet with the link and a line of text. */
    public static InlineKeyboardButton shareButton(String link, String text) {
        return InlineKeyboardButton.link("📤 ارسال برای دیگران", "https://t.me/share/url?url=" + encode(link)
                + "&text=" + encode(text));
    }

    /** The member id in a {@code /start} text such as "/start p42" or "/start@MyBot p42"; empty for anything else. */
    public static OptionalLong profileIdIn(String startText) {
        if (startText == null) {
            return OptionalLong.empty();
        }
        String[] parts = startText.strip().split("\\s+");
        if (parts.length != 2) {
            return OptionalLong.empty();
        }
        Matcher matcher = PROFILE_PAYLOAD.matcher(parts[1]);
        return matcher.matches() ? OptionalLong.of(Long.parseLong(matcher.group(1))) : OptionalLong.empty();
    }

    private String username() {
        if (botUsername == null) {
            try {
                botUsername = telegram.getMe().username();
            } catch (RuntimeException e) {
                return null;
            }
        }
        return botUsername;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
