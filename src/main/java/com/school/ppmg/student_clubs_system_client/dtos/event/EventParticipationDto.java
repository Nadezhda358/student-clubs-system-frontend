package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;

import java.time.OffsetDateTime;

public record EventParticipationDto(
        Long eventId,
        String eventTitle,
        Long clubId,
        String clubName,
        Long studentId,
        String studentFullName,
        String studentEmail,
        RegistrationStatus status,
        OffsetDateTime registeredAt,
        OffsetDateTime cancelledAt,
        EventStatus eventStatus,
        OffsetDateTime eventStartAt,
        OffsetDateTime eventEndAt
) {}
