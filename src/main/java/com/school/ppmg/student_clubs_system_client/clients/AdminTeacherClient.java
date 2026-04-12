package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.auth.UserDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "student-clubs-system",
        contextId = "adminTeacherClient",
        url = "${app.api.base-url}/api/admin/teachers"
)
public interface AdminTeacherClient {

    @GetMapping
    PageResponse<UserDto> getAllTeachers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    );
}
