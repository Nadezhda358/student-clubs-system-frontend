package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.announcement.AnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "announcementClient",
        url = "${app.api.base-url}/api/announcements"
)
public interface AnnouncementClient {

    @GetMapping
    PageResponse<AnnouncementDto> getPublicAnnouncements(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    AnnouncementDto getPublicAnnouncementById(@PathVariable Long id);
}
