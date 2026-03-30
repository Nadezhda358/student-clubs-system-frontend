package com.school.ppmg.student_clubs_system_client.dtos.club;

import jakarta.validation.constraints.Size;

public record CreateMembershipApplicationRequest(
        @Size(max = 2000) String motivationText
) {}
