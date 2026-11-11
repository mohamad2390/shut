package com.shut.browse;

/** One line in a list of members: enough to recognise them and open their page. */
public record ProviderSummary(Long userId, String displayName, String headline, String city) {
}
