package com.shut.telegram.conversation;

/** What the bot is waiting for from a user. Add a value for each new multi-step flow. */
public enum ConversationStep {
    AWAITING_PROFILE_FIELD,
    AWAITING_SHOWCASE_MEDIA,
    /** The optional name of a price, asked only for services that let the member name their own. */
    AWAITING_PRICE_TITLE,
    AWAITING_PRICE_UNIT,
    AWAITING_PRICE_AMOUNT,
    AWAITING_PRICE_EDIT_AMOUNT,
    AWAITING_SEARCH_QUERY,
    BROWSING_PROVIDERS,
    AWAITING_REVIEW_COMMENT,
    AWAITING_PROJECT_TITLE,
    AWAITING_PROJECT_CATEGORY,
    AWAITING_PROJECT_DESCRIPTION,
    AWAITING_PROJECT_CITY,
    AWAITING_PROJECT_BUDGET,
    AWAITING_PROJECT_DEADLINE,
    AWAITING_BID_AMOUNT,
    AWAITING_BID_NOTE,
    /** A type, brand or model of gear the member typed because it was not in the list. */
    AWAITING_EQUIPMENT_TYPE,
    AWAITING_EQUIPMENT_BRAND,
    AWAITING_EQUIPMENT_MODEL,
    /** The name of a new photography venue. */
    AWAITING_VENUE_NAME,
    /** The text of one venue field the owner is editing. */
    AWAITING_VENUE_FIELD,
    /** The amount of a new venue price, then its optional name; the unit was picked with a button. */
    AWAITING_VENUE_PRICE_AMOUNT,
    AWAITING_VENUE_PRICE_TITLE,
    AWAITING_VENUE_PHOTO,
    /** The note sent with a venue booking request. */
    AWAITING_BOOKING_NOTE
}
