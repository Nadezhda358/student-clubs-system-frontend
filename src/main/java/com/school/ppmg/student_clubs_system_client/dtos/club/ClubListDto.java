package com.school.ppmg.student_clubs_system_client.dtos.club;

public record ClubListDto(
        Long id,
        String name,
        String room,
        Boolean isActive
) {}
