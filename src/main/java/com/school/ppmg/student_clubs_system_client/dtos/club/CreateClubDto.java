package com.school.ppmg.student_clubs_system_client.dtos.club;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateClubDto(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 2000) String scheduleText,
        @Size(max = 80) String room,
        @Email @Size(max = 255) String contactEmail,
        @Pattern(
                regexp = "^$|^(?=.*\\d)\\+?[\\d()\\s-]+$",
                message = "contactPhone can contain only digits, spaces, parentheses, hyphens, and an optional leading +"
        )
        @Size(max = 40) String contactPhone,
        @NotNull Boolean isActive,
        List<@Positive Long> teacherIds
) implements ClubWriteRequest {}
