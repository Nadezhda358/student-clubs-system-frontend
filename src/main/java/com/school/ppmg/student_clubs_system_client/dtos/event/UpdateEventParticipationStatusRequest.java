package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventParticipationStatusRequest(
        @NotNull RegistrationStatus status
) {}
