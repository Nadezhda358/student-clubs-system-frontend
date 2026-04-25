package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.announcement.AnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.announcement.UpsertAnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "teacherAnnouncementClient",
        url = "${app.api.base-url}/api/teacher/announcements"
)
public interface TeacherAnnouncementClient {

    @GetMapping
    PageResponse<AnnouncementDto> getTeacherAnnouncements(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    AnnouncementDto getTeacherAnnouncementById(@PathVariable Long id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AnnouncementDto createTeacherAnnouncement(@Valid @RequestBody UpsertAnnouncementDto dto);

    @PutMapping("/{id}")
    AnnouncementDto updateTeacherAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody UpsertAnnouncementDto dto
    );

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTeacherAnnouncement(@PathVariable Long id);
}
