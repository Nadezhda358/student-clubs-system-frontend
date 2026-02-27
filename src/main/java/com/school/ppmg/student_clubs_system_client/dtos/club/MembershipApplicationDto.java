package com.school.ppmg.student_clubs_system_client.dtos.club;

import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;

import java.time.OffsetDateTime;

public record MembershipApplicationDto(
        Long id,
        Long clubId,
        String clubName,
        Long studentId,
        String studentFullName,
        String studentEmail,
        MembershipRequestStatus status,
        String motivationText,
        OffsetDateTime createdAt
) {}
