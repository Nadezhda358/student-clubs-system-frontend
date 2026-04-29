package com.school.ppmg.student_clubs_system_client.dtos.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertAnnouncementDto(
        @NotNull Long clubId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 8000) String body,
        @NotNull Boolean isPublished
) {
}
