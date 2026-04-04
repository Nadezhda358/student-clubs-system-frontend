package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;

import java.time.OffsetDateTime;

public record MyEventDto(
        Long eventId,
        Long clubId,
        String clubName,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String location,
        EventStatus eventStatus,
        EventAudience audience,
        RegistrationStatus registrationStatus,
        OffsetDateTime registeredAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime registrationDeadline,
        OffsetDateTime effectiveRegistrationDeadline
) {}
