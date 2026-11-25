package com.shut.api.me;

import com.shut.api.BadRequestException;
import com.shut.api.NotFoundException;
import com.shut.pricing.PriceItem;
import com.shut.pricing.PricingCatalog;
import com.shut.pricing.PricingService;
import com.shut.pricing.PricingUnit;
import com.shut.pricing.ServiceType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The price menu for the web app, under the same rules as the bot: 20 prices, services from the catalog. */
@Service
public class PricingApiService {

    static final int MAX_TITLE_LENGTH = 100;

    private final PricingService prices;
    private final PricingCatalog catalog;

    public PricingApiService(PricingService prices, PricingCatalog catalog) {
        this.prices = prices;
        this.catalog = catalog;
    }

    public PricingCatalogResponse catalog() {
        List<ServiceTypeResponse> services = catalog.services().stream()
                .map(service -> new ServiceTypeResponse(service.getId(), service.getName(), service.getIcon(),
                        service.asksName()))
                .toList();
        return new PricingCatalogResponse(services, units(), PricingService.MAX_ITEMS, PricingService.MAX_AMOUNT);
    }

    public List<PriceItemResponse> list(Long userId) {
        Map<PricingUnit, String> icons = catalog.unitIcons();
        return prices.list(userId).stream().map(item -> response(item, icons)).toList();
    }

    /**
     * @throws BadRequestException                     if the service, unit or amount is not valid
     * @throws com.shut.pricing.PriceLimitReachedException if the menu already has 20 prices
     */
    public PriceItemResponse add(Long userId, CreatePriceRequest request) {
        ServiceType service = request.serviceId() == null ? null : catalog.service(request.serviceId()).orElse(null);
        if (service == null) {
            throw new BadRequestException("Unknown service");
        }
        PricingUnit unit = unit(request.unit());
        long amount = amount(request.amount());
        String title = service.asksName() ? title(request.title()) : null;
        return response(prices.add(userId, service, title, unit, amount), catalog.unitIcons());
    }

    /**
     * @throws NotFoundException   if the price is not the member's
     * @throws BadRequestException if nothing is sent, or the unit or amount is not valid
     */
    public PriceItemResponse update(Long userId, Long id, UpdatePriceRequest request) {
        if (request.unit() == null && request.amount() == null) {
            throw new BadRequestException("Send a unit or an amount");
        }
        PricingUnit unit = request.unit() == null ? null : unit(request.unit());
        Long amount = request.amount() == null ? null : amount(request.amount());
        prices.findOwned(id, userId).orElseThrow(() -> notFound(id));
        if (amount != null) {
            prices.updateAmount(id, userId, amount);
        }
        if (unit != null) {
            prices.updateUnit(id, userId, unit);
        }
        return response(prices.findOwned(id, userId).orElseThrow(() -> notFound(id)), catalog.unitIcons());
    }

    /** @throws NotFoundException if the price is not the member's */
    public void delete(Long userId, Long id) {
        if (!prices.deleteOwned(id, userId)) {
            throw notFound(id);
        }
    }

    private List<UnitResponse> units() {
        Map<PricingUnit, String> icons = catalog.unitIcons();
        return Arrays.stream(PricingUnit.values()).map(unit -> unitResponse(unit, icons)).toList();
    }

    private static UnitResponse unitResponse(PricingUnit unit, Map<PricingUnit, String> icons) {
        return new UnitResponse(unit.name(), unit.phrase(), icons.get(unit));
    }

    private static PriceItemResponse response(PriceItem item, Map<PricingUnit, String> icons) {
        return new PriceItemResponse(item.getId(), item.getTitle(), item.getIcon(),
                item.getServiceType() == null ? null : item.getServiceType().getId(),
                unitResponse(item.getUnit(), icons), item.getAmount());
    }

    private static PricingUnit unit(String key) {
        return key == null ? invalid("A unit is required") : PricingUnit.fromKey(key).orElseGet(() -> invalid("Unknown unit"));
    }

    private static long amount(Long amount) {
        if (amount == null || amount <= 0 || amount > PricingService.MAX_AMOUNT) {
            throw new BadRequestException("The amount must be between 1 and " + PricingService.MAX_AMOUNT + " tomans");
        }
        return amount;
    }

    private static String title(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("The name can be at most " + MAX_TITLE_LENGTH + " characters");
        }
        return trimmed;
    }

    private static PricingUnit invalid(String message) {
        throw new BadRequestException(message);
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Price not found: " + id);
    }
}
