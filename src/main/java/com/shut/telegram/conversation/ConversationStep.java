package com.shut.telegram.conversation;

/** What the bot is waiting for from a user. Add a value for each new multi-step flow. */
public enum ConversationStep {
    AWAITING_PROFILE_FIELD,
    AWAITING_SHOWCASE_MEDIA,
    AWAITING_PRICE_TITLE,
    AWAITING_PRICE_UNIT,
    AWAITING_PRICE_AMOUNT
}
