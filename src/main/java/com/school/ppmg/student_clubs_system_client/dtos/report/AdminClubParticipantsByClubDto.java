package com.school.ppmg.student_clubs_system_client.dtos.report;

public record AdminClubParticipantsByClubDto(
        Long clubId,
        String clubName,
        Boolean active,
        Long participantsCount
) {}
