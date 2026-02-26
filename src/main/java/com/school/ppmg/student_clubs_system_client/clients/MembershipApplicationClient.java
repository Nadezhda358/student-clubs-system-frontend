package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.CreateMembershipApplicationRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "student-clubs-system",
        contextId = "membershipApplicationClient",
        url = "${app.api.base-url}/api"
)
public interface MembershipApplicationClient {
    @PostMapping("/clubs/{clubId}/membership-applications")
    MembershipApplicationDto apply(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateMembershipApplicationRequest request
    );

    @GetMapping("/me/membership-applications")
    List<MembershipApplicationDto> getMyApplications(
            @RequestParam(required = false) MembershipRequestStatus status
    );

}
