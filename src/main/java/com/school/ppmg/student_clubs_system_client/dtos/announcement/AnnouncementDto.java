package com.school.ppmg.student_clubs_system_client.dtos.announcement;

import java.time.OffsetDateTime;

public record AnnouncementDto(
        Long id,
        Long clubId,
        String clubName,
        String title,
        String body,
        Boolean isPublished,
        OffsetDateTime publishedAt,
        Long authorId,
        String authorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
