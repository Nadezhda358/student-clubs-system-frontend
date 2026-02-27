package com.school.ppmg.student_clubs_system_client.dtos.club;

import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipApplicationStatusRequest(
        @NotNull MembershipRequestStatus status
) {
}
