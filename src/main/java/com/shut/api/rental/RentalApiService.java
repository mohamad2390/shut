package com.shut.api.rental;

import com.shut.api.BadRequestException;
import com.shut.api.NotFoundException;
import com.shut.api.PageResponse;
import com.shut.rental.RentalItem;
import com.shut.rental.RentalItemPhoto;
import com.shut.rental.RentalService;
import com.shut.rental.RentalShop;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Service;


/**
 * The rental shops for the web app, on the bot's rules: a shop is visible with contact details and an available item;
 * an item only while it is available in a visible shop.
 */
@Service
public class RentalApiService {

    static final int MIN_SEARCH_LENGTH = 2;

    private final RentalService rental;
    private final UserService users;

    public RentalApiService(RentalService rental, UserService users) {
        this.rental = rental;
        this.users = users;
    }

    public PageResponse<RentalShopListItem> shops(int page, int size) {
        return PageResponse.of(rental.listed(page, size), shop -> new RentalShopListItem(shop.getId(), shop.getName(),
                shop.getCity(), rental.availableCount(shop.getId())));
    }

    /** @throws NotFoundException if the shop does not exist or is not visible */
    public RentalShopDetailsResponse shop(Long id) {
        RentalShop shop = rental.findListed(id).orElseThrow(() -> new NotFoundException("Shop not found: " + id));
        return new RentalShopDetailsResponse(shop.getId(), shop.getName(), shop.getPhone(), shop.getCity(),
                shop.getAddress(), shop.getDescription(), users.findById(shop.getOwnerId()).getTelegramUsername(),
                rental.availableItems(id).stream().map(this::summary).toList());
    }

    /** @throws BadRequestException if the text is shorter than two characters */
    public PageResponse<RentalSearchItem> search(String text, int page, int size) {
        if (text == null || text.strip().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException("Search text must be at least " + MIN_SEARCH_LENGTH + " characters");
        }
        return PageResponse.of(rental.search(text, page, size), item -> {
            RentalShop shop = rental.find(item.getShopId()).orElseThrow();
            return new RentalSearchItem(item.getId(), item.getTitle(), item.getModel().getType().getIcon(),
                    item.getModel().getType().getName(), item.getDailyPrice(), rental.photoCount(item.getId()),
                    shop.getId(), shop.getName(), shop.getCity());
        });
    }

    /** @throws NotFoundException if the item is not visible */
    public RentalItemDetailsResponse item(Long id) {
        RentalItem item = rental.publicItem(id).orElseThrow(() -> new NotFoundException("Item not found: " + id));
        RentalShop shop = rental.find(item.getShopId()).orElseThrow();
        return new RentalItemDetailsResponse(item.getId(), item.getTitle(), item.getModel().getType().getIcon(),
                item.getModel().getType().getName(), item.getDescription(), item.getDailyPrice(), shop.getId(),
                shop.getName(), shop.getPhone(), shop.getCity(), shop.getAddress(),
                users.findById(shop.getOwnerId()).getTelegramUsername(),
                rental.photos(id).stream().map(RentalItemPhoto::getId).toList());
    }

    private RentalItemSummary summary(RentalItem item) {
        return new RentalItemSummary(item.getId(), item.getTitle(), item.getModel().getType().getIcon(),
                item.getModel().getType().getName(), item.getDailyPrice(), rental.photoCount(item.getId()));
    }
}
