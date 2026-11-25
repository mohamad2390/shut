package com.shut.api.market;

import com.shut.api.venue.LabelResponse;
import com.shut.equipment.EquipmentType;
import com.shut.equipment.EquipmentTypeRepository;
import com.shut.market.Listing;
import com.shut.market.ListingCondition;
import com.shut.market.ListingPhoto;
import com.shut.market.MarketService;
import com.shut.market.OfferService;
import com.shut.telegram.messaging.BotLinks;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns listings into what the API returns. */
@Component
public class ListingViews {

    private final MarketService market;
    private final OfferService offers;
    private final EquipmentTypeRepository types;
    private final UserService users;
    private final ObjectProvider<BotLinks> botLinks;

    public ListingViews(MarketService market, OfferService offers, EquipmentTypeRepository types, UserService users,
                        ObjectProvider<BotLinks> botLinks) {
        this.market = market;
        this.offers = offers;
        this.types = types;
        this.users = users;
        this.botLinks = botLinks;
    }

    public List<CategoryItem> categories() {
        return types.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(type -> new CategoryItem(type.getId(), type.getName(), type.getIcon())).toList();
    }

    public Optional<CategoryItem> category(Long id) {
        return id == null ? Optional.empty()
                : types.findById(id).map(type -> new CategoryItem(type.getId(), type.getName(), type.getIcon()));
    }

    public static LabelResponse condition(ListingCondition condition) {
        return new LabelResponse(condition.name(), condition.label());
    }

    public List<ListingListItem> items(List<Listing> listings) {
        Map<Long, EquipmentType> byId = types.findAll().stream()
                .collect(Collectors.toMap(EquipmentType::getId, Function.identity()));
        return listings.stream().map(listing -> {
            List<ListingPhoto> photos = market.photos(listing.getId());
            EquipmentType type = listing.getCategoryId() == null ? null : byId.get(listing.getCategoryId());
            return new ListingListItem(listing.getId(), listing.getTitle(), condition(listing.getCondition()),
                    type == null ? null : type.getName(), listing.getBrand(), listing.getModel(), listing.getPrice(),
                    listing.isNegotiable(), listing.getCity(), photos.isEmpty() ? null : photos.get(0).getId());
        }).toList();
    }

    public ListingDetails details(Listing listing) {
        return new ListingDetails(listing.getId(), listing.getTitle(), listing.getDescription(),
                category(listing.getCategoryId()).orElse(null), listing.getBrand(), listing.getModel(),
                condition(listing.getCondition()), listing.getPrice(), listing.isNegotiable(), listing.getCity(),
                contact(listing.getSellerId()).name(),
                market.photos(listing.getId()).stream().map(ListingPhoto::getId).toList(), listing.getCreatedAt());
    }

    public MyListing mine(Listing listing) {
        BotLinks links = botLinks.getIfAvailable();
        return new MyListing(listing.getId(), listing.getTitle(), listing.getDescription(),
                category(listing.getCategoryId()).orElse(null), listing.getBrand(), listing.getModel(),
                condition(listing.getCondition()), listing.getPrice(), listing.isNegotiable(), listing.getCity(),
                new LabelResponse(listing.getStatus().name(), listing.getStatus().label()), listing.isHidden(),
                market.photoCount(listing.getId()), offers.pendingCount(listing.getId()),
                links == null ? null : links.botUrl().orElse(null));
    }

    public ContactResponse contact(Long userId) {
        User user = users.findById(userId);
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new ContactResponse(name.isEmpty() ? null : name, user.getTelegramUsername(), user.getPhoneNumber());
    }
}
