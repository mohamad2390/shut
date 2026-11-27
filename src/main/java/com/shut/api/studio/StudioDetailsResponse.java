package com.shut.api.studio;

import java.util.List;

/** Everything the studio page shows. */
public record StudioDetailsResponse(Long id, String name, String managerName, String phone, String city,
                                    String address, String description, String ownerTelegramUsername,
                                    List<StudioOfferingResponse> offerings, List<StudioMediaResponse> media,
                                    List<StudioPersonResponse> people) {
}
