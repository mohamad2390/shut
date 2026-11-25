package com.shut.telegram.business;

import com.shut.business.BusinessKind;
import com.shut.business.BusinessService;
import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.conversation.ConversationStep;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.venue.VenueService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Handles the business section's buttons: the category list, the venue entry, and the studio and rental cards. */
@Component
@TelegramBotEnabled
class BusinessCallbackHandler extends CallbackHandler {

    static final String KIND_KEY = "kind";

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final BusinessService business;
    private final VenueService venues;

    BusinessCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                            ConversationService conversations, TelegramAccountResolver accounts,
                            BusinessService business, VenueService venues) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.business = business;
        this.venues = venues;
    }

    @Override
    protected String prefix() {
        return BusinessCallbacks.PREFIX;
    }

    @Override
    protected void onCallback(CallbackContext context) {
        conversations.clear(context.user().id());
        User user = accounts.resolve(context.user());
        Screen screen = switch (context.arg(0)) {
            case "home" -> home(user);
            case "venue" -> venue(user);
            case "k" -> BusinessKind.fromKey(context.arg(1)).map(kind -> open(user, kind)).orElse(null);
            case "edit" -> BusinessKind.fromKey(context.arg(1)).map(kind -> startForm(context, kind)).orElse(null);
            default -> null;
        };
        if (screen != null) {
            messenger.show(context.chatId(), context.messageId(), screen);
        }
    }

    private Screen home(User user) {
        boolean venueOwner = venueOwner(user);
        return BusinessScreens.home(new BusinessScreens.Held(user.hasRole(RoleCodes.PROVIDER), venueOwner,
                user.hasRole(RoleCodes.STUDIO_OWNER), business.kindsOf(user.getId())));
    }

    /** Venues can also be created on the web, so owning one counts as the role even if it was never granted. */
    private boolean venueOwner(User user) {
        boolean owns = !venues.ownedBy(user.getId()).isEmpty();
        if (owns && !user.hasRole(RoleCodes.VENUE_OWNER)) {
            business.becomeVenueOwner(user);
        }
        return owns || user.hasRole(RoleCodes.VENUE_OWNER);
    }

    private Screen venue(User user) {
        business.becomeVenueOwner(user);
        return BusinessScreens.venue(!venues.ownedBy(user.getId()).isEmpty());
    }

    private Screen open(User user, BusinessKind kind) {
        return business.find(user.getId(), kind)
                .map(profile -> BusinessScreens.card(kind, profile))
                .orElseGet(() -> startFormFor(user, kind));
    }

    private Screen startForm(CallbackContext context, BusinessKind kind) {
        conversations.begin(context.user().id(), ConversationStep.AWAITING_BUSINESS_NAME, Map.of(KIND_KEY, kind.key()));
        return BusinessScreens.askName(kind);
    }

    private Screen startFormFor(User user, BusinessKind kind) {
        conversations.begin(user.getTelegramId(), ConversationStep.AWAITING_BUSINESS_NAME, Map.of(KIND_KEY, kind.key()));
        return BusinessScreens.askName(kind);
    }
}
