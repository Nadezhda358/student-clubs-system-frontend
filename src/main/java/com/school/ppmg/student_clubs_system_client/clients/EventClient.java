package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventListDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventParticipationDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.MyEventDto;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.EventTimeFilter;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "eventClient",
        url = "${app.api.base-url}/api"
)
public interface EventClient {

    @GetMapping("/events")
    PageResponse<EventListDto> getPublicEvents(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) EventTimeFilter timeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/events/{id}")
    EventDto getPublicEventById(@PathVariable Long id);

    @PostMapping("/events/{id}/registrations")
    EventParticipationDto register(@PathVariable Long id);

    @DeleteMapping("/events/{id}/registrations")
    EventParticipationDto cancelRegistration(@PathVariable Long id);

    @GetMapping("/me/events")
    PageResponse<MyEventDto> getMyEvents(
            @RequestParam(required = false) RegistrationStatus registrationStatus,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EventTimeFilter timeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );
}
