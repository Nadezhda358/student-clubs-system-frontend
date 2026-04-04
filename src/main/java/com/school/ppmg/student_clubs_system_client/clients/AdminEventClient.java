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
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "adminEventClient",
        url = "${app.api.base-url}/api/admin"
)
public interface AdminEventClient {

    @GetMapping("/events")
    PageResponse<EventListDto> getAdminEvents(
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

    @GetMapping("/events/{id}")
    EventDto getAdminEventById(@PathVariable Long id);

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    EventDto createAdminEvent(@Valid @RequestBody UpsertEventDto dto);

    @PutMapping("/events/{id}")
    EventDto updateAdminEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpsertEventDto dto
    );

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAdminEvent(@PathVariable Long id);

    @GetMapping("/events/{id}/participants")
    PageResponse<EventParticipationDto> getAdminParticipantsForEvent(
            @PathVariable Long id,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );

    @PatchMapping("/events/{eventId}/participants/{studentId}")
    EventParticipationDto updateAdminParticipationStatus(
            @PathVariable Long eventId,
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateEventParticipationStatusRequest request
    );

    @GetMapping("/event-participations")
    PageResponse<EventParticipationDto> getAdminParticipations(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) RegistrationStatus registrationStatus,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EventTimeFilter timeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );
}
