package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.report.AdminEventsByPeriodDto;
import com.school.ppmg.student_clubs_system_client.dtos.report.AdminClubParticipantsByClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.report.AdminReportsOverviewDto;
import com.school.ppmg.student_clubs_system_client.enums.ReportPeriod;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;

@FeignClient(
        name = "student-clubs-system",
        contextId = "adminReportClient",
        url = "${app.api.base-url}/api/admin/reports"
)
public interface AdminReportClient {

    @GetMapping("/overview")
    AdminReportsOverviewDto getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    );

    @GetMapping("/events-by-period")
    AdminEventsByPeriodDto getEventsByPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) ReportPeriod period
    );

    @GetMapping("/participants-by-club")
    java.util.List<AdminClubParticipantsByClubDto> getParticipantsByClub();
}
