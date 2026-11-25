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
    /** The member was asked to share their phone number with the button under the text box. */
    AWAITING_PHONE_CONTACT,
    /** The hours of a venue booking request, unless it is for the whole day. */
    AWAITING_BOOKING_TIME,
    /** The note sent with a venue booking request. */
    AWAITING_BOOKING_NOTE,
    /** Registering a studio (one answer after another), editing one of its details, a service, the showcase, an invitee. */
    AWAITING_STUDIO_WIZARD,
    AWAITING_STUDIO_FIELD,
    AWAITING_OFFERING,
    AWAITING_STUDIO_MEDIA,
    AWAITING_STUDIO_INVITEE,
    /** The name, city and description of an equipment rental card. */
    AWAITING_BUSINESS_NAME,
    AWAITING_BUSINESS_CITY,
    AWAITING_BUSINESS_DESCRIPTION,
    /** The title, then the price, of a new gear listing; its condition is picked with a button. */
    AWAITING_LISTING_TITLE,
    AWAITING_LISTING_PRICE,
    AWAITING_LISTING_CONDITION,
    /** The text of one listing detail the seller is editing. */
    AWAITING_LISTING_FIELD,
    AWAITING_LISTING_PHOTO,
    /** The amount of an offer on a listing, then the note sent with it. */
    AWAITING_OFFER_AMOUNT,
    AWAITING_OFFER_NOTE
}
