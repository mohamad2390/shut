package com.shut.common.web;

/**
 * Central registry of all HTTP route paths, grouped by feature.
 * Fields of a nested interface are implicitly public static final,
 * so they can be used directly inside annotations such as @RequestMapping.
 */
public interface ApiPaths {

    String API_V1 = "/api/v1";

    /** Users module. */
    interface Users {
        String BASE = API_V1 + "/users";
        String ONBOARDING = "/onboarding";
        String BY_ID = "/{id}";
    }

    /** Professional profile module (nested under a user). */
    interface Profiles {
        String BASE = Users.BASE + "/{userId}/professional-profile";
    }

    /** Public read-only view of the specialists, for the web app. */
    interface Providers {
        String BASE = API_V1 + "/providers";
        String BY_ID = "/{id}";
        String SHOWCASE = "/{id}/showcase";
        String SHOWCASE_CATEGORIES = "/{id}/showcase/categories";
        String SHOWCASE_MEDIA = "/{id}/showcase/{itemId}/media";
    }

    /** Public read-only view of the studios, for the web app. */
    interface Studios {
        String BASE = API_V1 + "/studios";
        String BY_ID = "/{id}";
        String MEDIA = "/{id}/media/{mediaId}";
    }

    /** Public read-only view of the equipment rental shops and their items, for the web app. */
    interface Rental {
        String BASE = API_V1 + "/rental";
        String SHOPS = "/shops";
        String SHOP = "/shops/{id}";
        String ITEMS = "/items";
        String ITEM = "/items/{id}";
        String ITEM_PHOTO = "/items/{id}/photos/{photoId}";
    }

    /** Public read-only view of the photography venues, for the web app. */
    interface Venues {
        String BASE = API_V1 + "/venues";
        String BY_ID = "/{id}";
        String CALENDAR = "/{id}/calendar";
        String PHOTO = "/{id}/photos/{photoId}";
        String BOOKINGS = "/{id}/bookings";
        String CATALOG = "/catalog";
    }

    /** Signing in to the web app with a phone number and a one-time code. */
    interface Auth {
        String BASE = API_V1 + "/auth";
        String OTP_REQUEST = "/otp/request";
        String OTP_VERIFY = "/otp/verify";
        String LOGOUT = "/logout";
    }

    /** The signed-in member. */
    interface Me {
        String BASE = API_V1 + "/me";
        String PROFILE = "/profile";
        String CALENDAR = "/calendar";
        String CALENDAR_DAY = "/calendar/{date}";
        String PRICES = "/prices";
        String PRICE = "/prices/{id}";
        String SHOWCASE = "/showcase";
        String BOOKINGS = "/bookings";
        String VENUES = "/venues";
        String VENUE = "/venues/{id}";
        String VENUE_PRICES = "/venues/{id}/prices";
        String VENUE_PRICE = "/venues/{id}/prices/{priceId}";
        String VENUE_CALENDAR = "/venues/{id}/calendar";
        String VENUE_CALENDAR_DAY = "/venues/{id}/calendar/{date}";
        String VENUE_REQUESTS = "/venues/{id}/requests";
        String VENUE_REQUEST = "/venues/{id}/requests/{bookingId}";
        String LISTINGS = "/listings";
        String LISTING = "/listings/{id}";
        String LISTING_STATUS = "/listings/{id}/status";
        String LISTING_OFFERS = "/listings/{id}/offers";
        String LISTING_OFFER = "/listings/{id}/offers/{offerId}";
        String OFFERS = "/offers";
        String OFFER = "/offers/{offerId}";
    }

    /** What a price can be for and charged per; public, so the forms can offer the choices. */
    /** Only for administrators, see {@code app.admin.phones}. */
    interface Admin {
        String BASE = API_V1 + "/admin";
        String ME = "/me";
        String USERS = "/users";
        String USER_ACTIVE = "/users/{id}/active";
        String VENUES = "/venues";
        String VENUE_ACTIVE = "/venues/{id}/active";
        String SERVICES = "/services";
        String SERVICE = "/services/{id}";
        String UNITS = "/units";
        String UNIT = "/units/{key}";
        String LISTINGS = "/listings";
        String LISTING_HIDDEN = "/listings/{id}/hidden";
    }

    /** The gear market: public browsing; offers and the seller's own listings are under {@link Me}. */
    interface Market {
        String BASE = API_V1 + "/market";
        String CATALOG = "/catalog";
        String LISTINGS = "/listings";
        String LISTING = "/listings/{id}";
        String LISTING_PHOTO = "/listings/{id}/photos/{photoId}";
        String LISTING_OFFERS = "/listings/{id}/offers";
    }

    interface Pricing {
        String CATALOG = API_V1 + "/pricing/catalog";
    }

    /** Operational / documentation endpoints. */
    interface Ops {
        String HEALTH = "/actuator/health";
        String SWAGGER_UI = "/swagger-ui/index.html";
    }
}
