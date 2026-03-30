package com.school.ppmg.student_clubs_system_client.dtos.auth;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {}
