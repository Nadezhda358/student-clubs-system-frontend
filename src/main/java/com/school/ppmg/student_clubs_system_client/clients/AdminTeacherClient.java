package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.auth.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "student-clubs-system",
        contextId = "adminTeacherClient",
        url = "${app.api.base-url}/api/admin/teachers"
)
public interface AdminTeacherClient {

    @GetMapping
    List<UserDto> getAllTeachers();
}
