package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.CreateMembershipApplicationRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpdateMembershipApplicationStatusRequest;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    PageResponse<MembershipApplicationDto> getMyApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );

    @PostMapping("/me/membership-applications/{id}/cancel")
    MembershipApplicationDto cancelMyApplication(@PathVariable Long id);

    @GetMapping("/admin/membership-applications")
    @PreAuthorize("hasRole('ADMIN')")
    PageResponse<MembershipApplicationDto> adminGetAll(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );

    @PostMapping("/admin/membership-applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    MembershipApplicationDto adminUpdateApplicationStatus(
            @PathVariable Long id,
            @RequestBody UpdateMembershipApplicationStatusRequest request
    );

    default MembershipApplicationDto adminApprove(Long id) {
        return adminUpdateApplicationStatus(
                id,
                new UpdateMembershipApplicationStatusRequest(MembershipRequestStatus.APPROVED)
        );
    }

    default MembershipApplicationDto adminReject(Long id) {
        return adminUpdateApplicationStatus(
                id,
                new UpdateMembershipApplicationStatusRequest(MembershipRequestStatus.REJECTED)
        );
    }

    @GetMapping("/teacher/membership-applications")
    @PreAuthorize("hasRole('TEACHER')")
    PageResponse<MembershipApplicationDto> teacherGetAllApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );

    @PostMapping("/teacher/membership-applications/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    MembershipApplicationDto teacherUpdateApplicationStatus(
            @PathVariable Long id,
            @RequestBody UpdateMembershipApplicationStatusRequest request
    );

    default MembershipApplicationDto teacherApprove(Long id) {
        return teacherUpdateApplicationStatus(
                id,
                new UpdateMembershipApplicationStatusRequest(MembershipRequestStatus.APPROVED)
        );
    }

    default MembershipApplicationDto teacherReject(Long id) {
        return teacherUpdateApplicationStatus(
                id,
                new UpdateMembershipApplicationStatusRequest(MembershipRequestStatus.REJECTED)
        );
    }
}
