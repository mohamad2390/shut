package com.shut.api.me.venue;

import java.time.LocalDate;
import java.time.LocalTime;

/** A booking request waiting for the owner's answer. */
public record OwnerRequestResponse(Long id, LocalDate date, LocalTime startTime, LocalTime endTime, boolean fullDay,
                                   String note, RequesterResponse requester) {
}
