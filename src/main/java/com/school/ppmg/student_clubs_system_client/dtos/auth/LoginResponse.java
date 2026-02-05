package com.school.ppmg.student_clubs_system_client.dtos.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserDto user
) {}
