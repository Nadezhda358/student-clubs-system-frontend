package com.school.ppmg.student_clubs_system_client.dtos.teacher;

public record TeacherInviteResponse(
        Long id,
        String email,
        String expiresAt
) {}
