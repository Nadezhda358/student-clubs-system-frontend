package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EventFormRequest {
    private Long clubId;
    private String title = "";
    private String description = "";
    private String startAt = "";
    private String endAt = "";
    private String location = "";
    private Integer capacity;
    private String registrationDeadline = "";
    private EventStatus status = EventStatus.DRAFT;
    private EventAudience audience = EventAudience.ALL_STUDENTS;
    private MultipartFile mainImage;
}
