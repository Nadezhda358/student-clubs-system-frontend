package com.school.ppmg.student_clubs_system_client.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterStudentRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Min(1) @Max(12) Integer grade,
        @Size(max = 20) String className
) {}
