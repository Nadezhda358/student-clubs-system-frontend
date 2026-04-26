package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.*;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.EventTimeFilter;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "teacherEventClient",
        url = "${app.api.base-url}/api/teacher/events"
)
public interface TeacherEventClient {

    @GetMapping
    PageResponse<EventListDto> getTeacherEvents(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) EventTimeFilter timeFilter,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    EventDto getTeacherEventById(@PathVariable Long id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventDto createTeacherEvent(@Valid @RequestBody UpsertEventDto dto);

    @PutMapping("/{id}")
    EventDto updateTeacherEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpsertEventDto dto
    );

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTeacherEvent(@PathVariable Long id);

    @PostMapping(value = "/{id}/main-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    EventDto uploadTeacherEventMainImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping("/{id}/participants")
    PageResponse<EventParticipationDto> getTeacherParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );

    @PostMapping("/{eventId}/participants/{studentId}")
    EventParticipationDto updateTeacherParticipationStatus(
            @PathVariable Long eventId,
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateEventParticipationStatusRequest request
    );
}
