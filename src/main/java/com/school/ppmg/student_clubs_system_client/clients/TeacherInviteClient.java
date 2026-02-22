package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.teacher.TeacherInviteBulkRequest;
import com.school.ppmg.student_clubs_system_client.dtos.teacher.TeacherInviteResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "student-clubs-system",
        contextId = "teacherInviteClient",
        url = "${app.api.base-url}/api/admin/teacher-invites"
)
public interface TeacherInviteClient {

    @PostMapping
    List<TeacherInviteResponse> createTeacherInvites(
            @Valid @RequestBody TeacherInviteBulkRequest request
    );
}
