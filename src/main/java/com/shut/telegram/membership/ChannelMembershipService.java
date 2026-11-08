package com.shut.telegram.membership;

import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.config.TelegramProperties;
import com.shut.telegram.model.ChatMember;
import org.springframework.stereotype.Service;

import java.util.Set;

/** Answers whether a Telegram user belongs to the community channel. */
@Service
@TelegramBotEnabled
public class ChannelMembershipService {

    private static final Set<String> MEMBER_STATUSES = Set.of("creator", "administrator", "member");
    private static final String RESTRICTED = "restricted";

    private final TelegramApiClient telegram;
    private final String channelId;

    public ChannelMembershipService(TelegramApiClient telegram, TelegramProperties properties) {
        this.telegram = telegram;
        this.channelId = properties.channelId();
    }

    /** False when no channel is configured, meaning the membership requirement is off. */
    public boolean isRequired() {
        return channelId != null && !channelId.isBlank();
    }

    public boolean isMember(long userId) {
        if (!isRequired()) {
            return true;
        }
        ChatMember member = telegram.getChatMember(channelId, userId);
        if (RESTRICTED.equals(member.status())) {
            return Boolean.TRUE.equals(member.joined());
        }
        return MEMBER_STATUSES.contains(member.status());
    }
}
