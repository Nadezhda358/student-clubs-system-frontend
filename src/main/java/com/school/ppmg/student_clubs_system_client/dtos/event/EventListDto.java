package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;

import java.time.OffsetDateTime;

public record EventListDto(
        Long id,
        Long clubId,
        String clubName,
        String title,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String location,
        String mainImageUrl,
        Integer capacity,
        Long registeredCount,
        Long availableSpots,
        OffsetDateTime registrationDeadline,
        OffsetDateTime effectiveRegistrationDeadline,
        Boolean registrationOpen,
        EventStatus status,
        EventAudience audience
) {}
