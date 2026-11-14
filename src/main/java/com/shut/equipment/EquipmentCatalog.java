package com.shut.equipment;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The reference list of gear: types, brands and models. A name a member types that is not in the list is added to it,
 * so the list grows as members use it. Names are compared by {@link EquipmentNames#key}, so spelling variants of
 * something we have already are found, not added again.
 */
@Service
public class EquipmentCatalog {

    /** How many brands or models are offered as buttons. */
    public static final int CHOICES = 24;

    private final EquipmentTypeRepository types;
    private final EquipmentBrandRepository brands;
    private final EquipmentBrandAliasRepository aliases;
    private final EquipmentModelRepository models;

    public EquipmentCatalog(EquipmentTypeRepository types, EquipmentBrandRepository brands,
                            EquipmentBrandAliasRepository aliases, EquipmentModelRepository models) {
        this.types = types;
        this.brands = brands;
        this.aliases = aliases;
        this.models = models;
    }

    @Transactional(readOnly = true)
    public List<EquipmentType> types() {
        return types.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentType> type(long id) {
        return types.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentBrand> brand(long id) {
        return brands.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentModel> model(long id) {
        return models.findById(id);
    }

    /** Brands to offer for a type: the ones with the most models of it first. */
    @Transactional(readOnly = true)
    public List<EquipmentBrand> brandsFor(long typeId) {
        return brands.brandsFor(typeId, PageRequest.of(0, CHOICES));
    }

    @Transactional(readOnly = true)
    public List<EquipmentModel> modelsFor(long typeId, long brandId) {
        return models.modelsFor(typeId, brandId, PageRequest.of(0, CHOICES));
    }

    /** @throws IllegalArgumentException if the name has no letters or digits */
    @Transactional
    public Resolved<EquipmentType> typeNamed(String name, Long userId) {
        String key = EquipmentNames.key(name);
        return types.findByNormalizedName(key)
                .map(found -> new Resolved<>(found, false))
                .orElseGet(() -> new Resolved<>(types.save(new EquipmentType(name, userId)), true));
    }

    /** A brand is found by its name or by one of its other spellings. */
    @Transactional
    public Resolved<EquipmentBrand> brandNamed(String name, Long userId) {
        String key = EquipmentNames.key(name);
        Optional<EquipmentBrand> found = brands.findByNormalizedName(key)
                .or(() -> aliases.findByNormalizedAlias(key).flatMap(alias -> brands.findById(alias.getBrandId())));
        return found.map(brand -> new Resolved<>(brand, false))
                .orElseGet(() -> new Resolved<>(brands.save(new EquipmentBrand(name, userId)), true));
    }

    /**
     * The product with this type, brand and model name. The same model name under another brand or type is another
     * product.
     *
     * @throws IllegalArgumentException if the type or brand does not exist, or the name has no letters or digits
     */
    @Transactional
    public Resolved<EquipmentModel> modelNamed(long typeId, long brandId, String name, Long userId) {
        String key = EquipmentNames.key(name);
        EquipmentType type = types.findById(typeId).orElseThrow(() -> new IllegalArgumentException("No type " + typeId));
        EquipmentBrand brand = brands.findById(brandId).orElseThrow(() -> new IllegalArgumentException("No brand " + brandId));
        return models.findByType_IdAndBrand_IdAndNormalizedName(typeId, brandId, key)
                .map(found -> new Resolved<>(found, false))
                .orElseGet(() -> new Resolved<>(models.save(new EquipmentModel(type, brand, name, userId)), true));
    }
}
