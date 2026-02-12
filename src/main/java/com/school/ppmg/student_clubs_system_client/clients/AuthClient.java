package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.auth.LoginRequest;
import com.school.ppmg.student_clubs_system_client.dtos.auth.LoginResponse;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterStudentRequest;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterTeacherRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "student-clubs-system",
        contextId = "authClient",
        url = "${app.api.base-url}/api/auth"
)
public interface AuthClient {

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/register")
    void registerStudent(@Valid @RequestBody RegisterStudentRequest request);

    @PostMapping("/register/teacher")
    void registerTeacher(@Valid @RequestBody RegisterTeacherRequest request);
}
