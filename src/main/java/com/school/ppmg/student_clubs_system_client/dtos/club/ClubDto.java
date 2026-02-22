package com.school.ppmg.student_clubs_system_client.dtos.club;

import java.time.OffsetDateTime;
import java.util.List;

public record ClubDto(
        Long id,
        String name,
        String description,
        String scheduleText,
        String room,
        String contactEmail,
        String contactPhone,
        String mainImageUrl,
        Boolean isActive,

        Long createdById,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        List<TeacherDto> teachers,
        List<MediaDto> media
) {}
