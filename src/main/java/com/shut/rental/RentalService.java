package com.shut.rental;

import com.shut.equipment.EquipmentModel;
import com.shut.equipment.EquipmentModelRepository;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Rental shops, the items they rent out and the items' photos. Every change checks the shop is the member's. */
@Service
public class RentalService {

    public static final int MAX_ITEMS = 40;
    public static final int MAX_PHOTOS_PER_ITEM = 3;

    /** Whether a product from the catalog can be added to the member's shop. */
    public enum ItemCheck { OK, NO_SHOP, NO_PRODUCT, ALREADY_LISTED, LIMIT_REACHED }

    /** What happened when a photo was added. */
    public enum PhotoResult { ADDED, DUPLICATE, LIMIT_REACHED, NOT_FOUND }

    private final RentalShopRepository shops;
    private final RentalItemRepository items;
    private final RentalItemPhotoRepository photos;
    private final EquipmentModelRepository models;
    private final RoleService roles;

    public RentalService(RentalShopRepository shops, RentalItemRepository items, RentalItemPhotoRepository photos,
                         EquipmentModelRepository models, RoleService roles) {
        this.shops = shops;
        this.items = items;
        this.photos = photos;
        this.models = models;
        this.roles = roles;
    }

    /** Starts the member's shop and makes them an equipment renter. Empty if they already have one. */
    @Transactional
    public Optional<RentalShop> create(User owner, String name) {
        if (shops.findByOwnerId(owner.getId()).isPresent()) {
            return Optional.empty();
        }
        roles.grant(owner, RoleCodes.EQUIPMENT_RENTER);
        return Optional.of(shops.save(new RentalShop(owner.getId(), name)));
    }

    @Transactional(readOnly = true)
    public Optional<RentalShop> find(Long id) {
        return shops.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<RentalShop> findListed(Long id) {
        return shops.findById(id).filter(this::isListed);
    }

    @Transactional(readOnly = true)
    public Optional<RentalShop> ownedBy(Long ownerId) {
        return shops.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public boolean isListed(RentalShop shop) {
        return shop.isActive() && shop.hasContactDetails() && items.countByShopIdAndAvailableTrue(shop.getId()) > 0;
    }

    @Transactional(readOnly = true)
    public Page<RentalShop> listed(int page, int size) {
        return shops.listed(PageRequest.of(Math.max(page, 0), size));
    }

    @Transactional
    public Optional<RentalShop> update(Long ownerId, Consumer<RentalShop> change) {
        return shops.findByOwnerId(ownerId).map(shop -> {
            change.accept(shop);
            return shops.save(shop);
        });
    }

    /** Deletes the owner's shop with its items and returns every item photo so the backups can be removed. */
    @Transactional
    public Optional<List<RentalItemPhoto>> delete(Long ownerId) {
        return shops.findByOwnerId(ownerId).map(shop -> {
            List<Long> itemIds = items.findByShopIdOrderByIdAsc(shop.getId()).stream().map(RentalItem::getId).toList();
            List<RentalItemPhoto> removed = itemIds.isEmpty() ? List.of() : photos.findByItemIdIn(itemIds);
            shops.delete(shop);
            return removed;
        });
    }

    /** Every item of the shop, for its owner. */
    @Transactional(readOnly = true)
    public List<RentalItem> items(Long shopId) {
        return items.findByShopIdOrderByIdAsc(shopId);
    }

    /** The items customers can ask about. */
    @Transactional(readOnly = true)
    public List<RentalItem> availableItems(Long shopId) {
        return items.findByShopIdAndAvailableTrueOrderByIdAsc(shopId);
    }

    @Transactional(readOnly = true)
    public long availableCount(Long shopId) {
        return items.countByShopIdAndAvailableTrue(shopId);
    }

    @Transactional(readOnly = true)
    public Optional<RentalItem> item(Long itemId) {
        return items.findById(itemId);
    }

    /** The item if it belongs to the member's shop. */
    @Transactional(readOnly = true)
    public Optional<RentalItem> ownedItem(Long ownerId, Long itemId) {
        return shops.findByOwnerId(ownerId).flatMap(shop -> items.findByIdAndShopId(itemId, shop.getId()));
    }

    /** The item if customers can see it: available, in a listed shop. */
    @Transactional(readOnly = true)
    public Optional<RentalItem> publicItem(Long itemId) {
        return items.findById(itemId).filter(RentalItem::isAvailable)
                .filter(item -> shops.findById(item.getShopId()).filter(this::isListed).isPresent());
    }

    /** Whether the product can be added to the member's shop: it exists, is not there yet, and there is room. */
    @Transactional(readOnly = true)
    public ItemCheck checkCanAddItem(Long ownerId, Long modelId) {
        Optional<RentalShop> shop = shops.findByOwnerId(ownerId);
        if (shop.isEmpty()) {
            return ItemCheck.NO_SHOP;
        }
        if (!models.existsById(modelId)) {
            return ItemCheck.NO_PRODUCT;
        }
        if (items.existsByShopIdAndModel_Id(shop.get().getId(), modelId)) {
            return ItemCheck.ALREADY_LISTED;
        }
        return items.countByShopId(shop.get().getId()) >= MAX_ITEMS ? ItemCheck.LIMIT_REACHED : ItemCheck.OK;
    }

    /** Adds the catalog product to the owner's shop with its daily price. Empty unless {@link #checkCanAddItem} is OK. */
    @Transactional
    public Optional<RentalItem> addItem(Long ownerId, Long modelId, String description, long dailyPrice) {
        if (checkCanAddItem(ownerId, modelId) != ItemCheck.OK) {
            return Optional.empty();
        }
        EquipmentModel model = models.findById(modelId).orElseThrow();
        Long shopId = shops.findByOwnerId(ownerId).orElseThrow().getId();
        return Optional.of(items.save(new RentalItem(shopId, model, description, dailyPrice)));
    }

    @Transactional
    public Optional<RentalItem> updateItem(Long ownerId, Long itemId, Consumer<RentalItem> change) {
        return ownedItem(ownerId, itemId).map(item -> {
            change.accept(item);
            return items.save(item);
        });
    }

    /** Removes the item and returns its photos so the backups can be removed. */
    @Transactional
    public Optional<List<RentalItemPhoto>> removeItem(Long ownerId, Long itemId) {
        return ownedItem(ownerId, itemId).map(item -> {
            List<RentalItemPhoto> removed = photos.findByItemIdOrderByIdAsc(item.getId());
            items.delete(item);
            return removed;
        });
    }

    @Transactional(readOnly = true)
    public Page<RentalItem> search(String text, int page, int size) {
        String pattern = "%" + text.strip().toLowerCase().replace("%", "").replace("_", "") + "%";
        return items.search(pattern, PageRequest.of(Math.max(page, 0), size));
    }

    @Transactional(readOnly = true)
    public List<RentalItemPhoto> photos(Long itemId) {
        return photos.findByItemIdOrderByIdAsc(itemId);
    }

    @Transactional(readOnly = true)
    public long photoCount(Long itemId) {
        return photos.countByItemId(itemId);
    }

    @Transactional(readOnly = true)
    public PhotoResult checkCanAddPhoto(Long ownerId, Long itemId, String fileUniqueId) {
        if (ownedItem(ownerId, itemId).isEmpty()) {
            return PhotoResult.NOT_FOUND;
        }
        if (photos.existsByItemIdAndFileUniqueId(itemId, fileUniqueId)) {
            return PhotoResult.DUPLICATE;
        }
        return photos.countByItemId(itemId) >= MAX_PHOTOS_PER_ITEM ? PhotoResult.LIMIT_REACHED : PhotoResult.ADDED;
    }

    @Transactional
    public PhotoResult addPhoto(Long ownerId, Long itemId, String fileId, String fileUniqueId, Long backupMessageId) {
        PhotoResult check = checkCanAddPhoto(ownerId, itemId, fileUniqueId);
        if (check == PhotoResult.ADDED) {
            photos.save(new RentalItemPhoto(itemId, fileId, fileUniqueId, backupMessageId));
        }
        return check;
    }

    @Transactional
    public List<RentalItemPhoto> clearPhotos(Long ownerId, Long itemId) {
        if (ownedItem(ownerId, itemId).isEmpty()) {
            return List.of();
        }
        List<RentalItemPhoto> removed = photos.findByItemIdOrderByIdAsc(itemId);
        photos.deleteAll(removed);
        return removed;
    }
}
