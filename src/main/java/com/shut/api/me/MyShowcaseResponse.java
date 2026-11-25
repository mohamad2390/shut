package com.shut.api.me;

import com.shut.api.showcase.ShowcaseCategoryResponse;

import java.util.List;

/**
 * What the member has in their showcase. It can only be filled in the Telegram bot; {@code botUrl} opens it, or is null
 * when the bot's address is not known right now.
 */
public record MyShowcaseResponse(List<ShowcaseCategoryResponse> categories, long total, String botUrl) {
}
