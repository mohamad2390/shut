package com.shut.telegram.business;

import com.shut.telegram.account.TelegramAccountResolver;
import com.shut.telegram.client.TelegramApiClient;
import com.shut.telegram.config.TelegramBotEnabled;
import com.shut.telegram.conversation.ConversationService;
import com.shut.telegram.handler.CallbackContext;
import com.shut.telegram.handler.CallbackHandler;
import com.shut.telegram.membership.MembershipGate;
import com.shut.telegram.messaging.BotMessenger;
import com.shut.telegram.messaging.Screen;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.service.RoleService;
import com.shut.venue.VenueService;
import org.springframework.stereotype.Component;

/** Handles the business section's buttons: the list of categories and the venue entry. */
@Component
@TelegramBotEnabled
class BusinessCallbackHandler extends CallbackHandler {

    private final BotMessenger messenger;
    private final ConversationService conversations;
    private final TelegramAccountResolver accounts;
    private final RoleService roles;
    private final VenueService venues;

    BusinessCallbackHandler(TelegramApiClient telegram, MembershipGate membershipGate, BotMessenger messenger,
                            ConversationService conversations, TelegramAccountResolver accounts,
                            RoleService roles, VenueService venues) {
        super(telegram, membershipGate);
        this.messenger = messenger;
        this.conversations = conversations;
        this.accounts = accounts;
        this.roles = roles;
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
            default -> null;
        };
        if (screen != null) {
            messenger.show(context.chatId(), context.messageId(), screen);
        }
    }

    private Screen home(User user) {
        return BusinessScreens.home(new BusinessScreens.Held(user.hasRole(RoleCodes.PROVIDER), venueOwner(user),
                user.hasRole(RoleCodes.STUDIO_OWNER), user.hasRole(RoleCodes.EQUIPMENT_RENTER)));
    }

    /** Venues can also be created on the web, so owning one counts as the role even if it was never granted. */
    private boolean venueOwner(User user) {
        boolean owns = !venues.ownedBy(user.getId()).isEmpty();
        if (owns && !user.hasRole(RoleCodes.VENUE_OWNER)) {
            roles.grant(user, RoleCodes.VENUE_OWNER);
        }
        return owns || user.hasRole(RoleCodes.VENUE_OWNER);
    }

    private Screen venue(User user) {
        roles.grant(user, RoleCodes.VENUE_OWNER);
        return BusinessScreens.venue(!venues.ownedBy(user.getId()).isEmpty());
    }
}
