package com.shut.api.market;

import java.time.OffsetDateTime;

public record SellerOfferResponse(Long id, Long amount, String message, OffsetDateTime createdAt,
                                  ContactResponse buyer) {
}
