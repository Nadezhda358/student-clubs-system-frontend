package com.school.ppmg.student_clubs_system_client.dtos.club;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertClubDto(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 2000) String scheduleText,
        @Size(max = 80) String room,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 40) String contactPhone,
        @NotNull Boolean isActive,
        Long createdById
) {}
