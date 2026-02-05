package com.school.ppmg.student_clubs_system_client.dtos.teacher;

import java.util.List;

public record TeacherInviteBulkRequest(
        List<String> emails
) {}
