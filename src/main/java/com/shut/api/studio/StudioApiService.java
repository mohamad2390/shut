package com.shut.api.studio;

import com.shut.api.NotFoundException;
import com.shut.api.PageResponse;
import com.shut.studio.Studio;
import com.shut.studio.StudioMemberService;
import com.shut.studio.StudioOffering;
import com.shut.studio.StudioService;
import com.shut.user.entity.User;
import com.shut.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The studios for the web app, on the same visibility rules as the bot: active, contact details, a package. */
@Service
public class StudioApiService {

    private final StudioService studios;
    private final StudioMemberService members;
    private final UserService users;

    public StudioApiService(StudioService studios, StudioMemberService members, UserService users) {
        this.studios = studios;
        this.members = members;
        this.users = users;
    }

    public PageResponse<StudioListItem> list(int page, int size) {
        return PageResponse.of(studios.listed(page, size), studio -> new StudioListItem(studio.getId(),
                studio.getName(), studio.getCity(), studios.offeringCount(studio.getId()),
                studios.mediaCount(studio.getId())));
    }

    /** @throws NotFoundException if the studio does not exist or is not visible */
    public StudioDetailsResponse details(Long id) {
        Studio studio = studios.findListed(id).orElseThrow(() -> new NotFoundException("Studio not found: " + id));
        List<StudioOffering> offerings = studios.offerings(id);
        Map<Long, String> labels = studios.serviceLabels(offerings);
        return new StudioDetailsResponse(studio.getId(), studio.getName(), studio.getManagerName(), studio.getPhone(),
                studio.getCity(), studio.getAddress(), studio.getDescription(),
                users.findById(studio.getOwnerId()).getTelegramUsername(),
                offerings.stream().map(offering -> new StudioOfferingResponse(offering.getId(), offering.getTitle(),
                        offering.getServiceIds().stream().sorted()
                                .map(serviceId -> labels.getOrDefault(serviceId, "")).filter(label -> !label.isEmpty())
                                .toList(), offering.getPrice())).toList(),
                studios.media(id).stream()
                        .map(media -> new StudioMediaResponse(media.getId(), media.getKind().name())).toList(),
                members.people(id).stream()
                        .map(member -> new StudioPersonResponse(member.getUserId(), name(member.getUserId())))
                        .sorted(Comparator.comparing(StudioPersonResponse::name)).toList());
    }

    private String name(Long userId) {
        User user = users.findById(userId);
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? "یک متخصص" : name;
    }
}
