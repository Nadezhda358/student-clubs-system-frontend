package com.school.ppmg.student_clubs_system_client.dtos.report;

import java.time.OffsetDateTime;

public record AdminReportsOverviewDto(
        Long totalClubs,
        Long activeClubs,
        Long inactiveClubs,
        Long activeMembers,
        Long totalEvents,
        Long registeredParticipations,
        Long cancelledParticipations,
        Long uniqueRegisteredParticipants,
        OffsetDateTime from,
        OffsetDateTime to
) {}
