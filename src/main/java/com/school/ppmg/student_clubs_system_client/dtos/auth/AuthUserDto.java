package com.school.ppmg.student_clubs_system_client.dtos.auth;

import com.school.ppmg.student_clubs_system_client.enums.UserRole;

public record AuthUserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {}
