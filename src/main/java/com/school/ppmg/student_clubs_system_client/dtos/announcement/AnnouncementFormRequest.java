package com.school.ppmg.student_clubs_system_client.dtos.announcement;

import lombok.Data;

@Data
public class AnnouncementFormRequest {
    private Long clubId;
    private String title = "";
    private String body = "";
    private Boolean published = Boolean.FALSE;
}
