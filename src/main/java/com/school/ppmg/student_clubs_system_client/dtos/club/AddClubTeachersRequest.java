package com.school.ppmg.student_clubs_system_client.dtos.club;

import jakarta.validation.constraints.Positive;

import java.util.List;

public record AddClubTeachersRequest(
        List<@Positive Long> teacherIds
) {}
