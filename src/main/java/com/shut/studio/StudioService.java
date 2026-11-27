package com.shut.studio;

import com.shut.pricing.ServiceType;
import com.shut.pricing.ServiceTypeRepository;
import com.shut.user.entity.RoleCodes;
import com.shut.user.entity.User;
import com.shut.user.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Studios, their services and their showcase. Every change checks that the studio belongs to the member making it. */
@Service
public class StudioService {

    public static final int MAX_OFFERINGS = 15;
    public static final int MAX_MEDIA = 12;

    /** What happened when a photo or video was added. */
    public enum MediaResult { ADDED, DUPLICATE, LIMIT_REACHED, NOT_FOUND }

    private final StudioRepository studios;
    private final StudioOfferingRepository offerings;
    private final StudioMediaRepository media;
    private final ServiceTypeRepository serviceTypes;
    private final RoleService roles;

    public StudioService(StudioRepository studios, StudioOfferingRepository offerings, StudioMediaRepository media,
                         ServiceTypeRepository serviceTypes, RoleService roles) {
        this.studios = studios;
        this.offerings = offerings;
        this.media = media;
        this.serviceTypes = serviceTypes;
        this.roles = roles;
    }

    /** Starts the member's studio and makes them a studio owner. Empty if they already have one. */
    @Transactional
    public Optional<Studio> create(User owner, String name) {
        if (studios.findByOwnerId(owner.getId()).isPresent()) {
            return Optional.empty();
        }
        roles.grant(owner, RoleCodes.STUDIO_OWNER);
        return Optional.of(studios.save(new Studio(owner.getId(), name)));
    }

    @Transactional(readOnly = true)
    public Optional<Studio> find(Long id) {
        return studios.findById(id);
    }

    /** The studio if others can see it. */
    @Transactional(readOnly = true)
    public Optional<Studio> findListed(Long id) {
        return studios.findById(id).filter(this::isListed);
    }

    @Transactional(readOnly = true)
    public Optional<Studio> ownedBy(Long ownerId) {
        return studios.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public boolean isListed(Studio studio) {
        return studio.isActive() && studio.hasContactDetails() && offerings.existsByStudioId(studio.getId());
    }

    @Transactional(readOnly = true)
    public Page<Studio> listed(int page, int size) {
        return studios.listed(PageRequest.of(Math.max(page, 0), size));
    }

    /** Applies the change to the owner's studio. Empty if the member has no studio. */
    @Transactional
    public Optional<Studio> update(Long ownerId, Consumer<Studio> change) {
        return studios.findByOwnerId(ownerId).map(studio -> {
            change.accept(studio);
            return studios.save(studio);
        });
    }

    /** Deletes the owner's studio with everything in it and returns its media so their backups can be removed. */
    @Transactional
    public Optional<List<StudioMedia>> delete(Long ownerId) {
        return studios.findByOwnerId(ownerId).map(studio -> {
            List<StudioMedia> removed = media.findByStudioIdOrderByIdAsc(studio.getId());
            studios.delete(studio);
            return removed;
        });
    }

    @Transactional(readOnly = true)
    public List<StudioOffering> offerings(Long studioId) {
        return offerings.findByStudioIdOrderByIdAsc(studioId);
    }

    @Transactional(readOnly = true)
    public long offeringCount(Long studioId) {
        return offerings.countByStudioId(studioId);
    }

    /** The services of the catalog a package can include: the ones that are switched on. */
    @Transactional(readOnly = true)
    public List<ServiceType> selectableServices() {
        return serviceTypes.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    /** The icon and name of every service the packages include, by id, also for services since switched off. */
    @Transactional(readOnly = true)
    public Map<Long, String> serviceLabels(Collection<StudioOffering> offerings) {
        List<Long> ids = offerings.stream().flatMap(offering -> offering.getServiceIds().stream()).distinct().toList();
        return serviceTypes.findAllById(ids).stream()
                .collect(Collectors.toMap(ServiceType::getId, service -> service.getIcon() + " " + service.getName()));
    }

    /**
     * Adds a package to the owner's studio, made of services from the catalog. Services that are not in the catalog or
     * are switched off are ignored. Empty if they have no studio, reached the limit, or no service is left.
     */
    @Transactional
    public Optional<StudioOffering> addOffering(Long ownerId, String title, Collection<Long> serviceIds, Long price) {
        var chosen = new LinkedHashSet<Long>();
        serviceTypes.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(ServiceType::getId).filter(serviceIds::contains).forEach(chosen::add);
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        return studios.findByOwnerId(ownerId)
                .filter(studio -> offerings.countByStudioId(studio.getId()) < MAX_OFFERINGS)
                .map(studio -> offerings.save(new StudioOffering(studio.getId(), title, chosen, price)));
    }

    @Transactional
    public boolean removeOffering(Long ownerId, Long offeringId) {
        return studios.findByOwnerId(ownerId)
                .flatMap(studio -> offerings.findByIdAndStudioId(offeringId, studio.getId()))
                .map(offering -> {
                    offerings.delete(offering);
                    return true;
                }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<StudioMedia> media(Long studioId) {
        return media.findByStudioIdOrderByIdAsc(studioId);
    }

    @Transactional(readOnly = true)
    public long mediaCount(Long studioId) {
        return media.countByStudioId(studioId);
    }

    @Transactional(readOnly = true)
    public MediaResult checkCanAddMedia(Long ownerId, String fileUniqueId) {
        Optional<Studio> studio = studios.findByOwnerId(ownerId);
        if (studio.isEmpty()) {
            return MediaResult.NOT_FOUND;
        }
        if (media.existsByStudioIdAndFileUniqueId(studio.get().getId(), fileUniqueId)) {
            return MediaResult.DUPLICATE;
        }
        return media.countByStudioId(studio.get().getId()) >= MAX_MEDIA ? MediaResult.LIMIT_REACHED : MediaResult.ADDED;
    }

    @Transactional
    public MediaResult addMedia(Long ownerId, MediaKind kind, String fileId, String fileUniqueId, Long backupMessageId) {
        MediaResult check = checkCanAddMedia(ownerId, fileUniqueId);
        if (check == MediaResult.ADDED) {
            Long studioId = studios.findByOwnerId(ownerId).orElseThrow().getId();
            media.save(new StudioMedia(studioId, kind, fileId, fileUniqueId, backupMessageId));
        }
        return check;
    }

    /** Removes the whole showcase of the owner's studio and returns it so the backups can be removed. */
    @Transactional
    public List<StudioMedia> clearMedia(Long ownerId) {
        return studios.findByOwnerId(ownerId).map(studio -> {
            List<StudioMedia> removed = media.findByStudioIdOrderByIdAsc(studio.getId());
            media.deleteAll(removed);
            return removed;
        }).orElse(List.of());
    }
}
