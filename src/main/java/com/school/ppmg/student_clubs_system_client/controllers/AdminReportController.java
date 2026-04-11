package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.AdminReportClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.report.AdminEventsByPeriodDto;
import com.school.ppmg.student_clubs_system_client.dtos.report.AdminEventsByPeriodPointDto;
import com.school.ppmg.student_clubs_system_client.dtos.report.AdminReportsOverviewDto;
import com.school.ppmg.student_clubs_system_client.enums.ReportPeriod;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class AdminReportController {
    private final AdminReportClient adminReportClient;

    @GetMapping({"/admin/stats", "/admin/reports"})
    public String adminReports(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) ReportPeriod period,
            Model model
    ) {
        OffsetDateTime from = EventViewSupport.parseFromDate(fromDate);
        OffsetDateTime to = EventViewSupport.parseToDate(toDate);
        ReportPeriod selectedPeriod = period == null ? ReportPeriod.MONTH : period;

        model.addAttribute("fromDate", fromDate == null ? "" : fromDate.trim());
        model.addAttribute("toDate", toDate == null ? "" : toDate.trim());
        model.addAttribute("selectedPeriod", selectedPeriod);
        model.addAttribute("periodValues", ReportPeriod.values());
        model.addAttribute("eventsByPeriodPoints", List.of());
        model.addAttribute("eventsByPeriodMaxCount", 0L);

        AdminReportsOverviewDto overview = null;
        try {
            overview = adminReportClient.getOverview(from, to);
            model.addAttribute("overview", overview);
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("overviewErrorMessage", toOverviewLoadErrorMessage(ex));
        }

        AdminEventsByPeriodDto eventsByPeriod = null;
        try {
            eventsByPeriod = adminReportClient.getEventsByPeriod(from, to, selectedPeriod);
            List<AdminEventsByPeriodPointDto> points = eventsByPeriod.points() == null
                    ? List.of()
                    : eventsByPeriod.points();
            model.addAttribute("eventsByPeriod", eventsByPeriod);
            model.addAttribute("eventsByPeriodPoints", points);
            model.addAttribute(
                    "eventsByPeriodMaxCount",
                    points.stream()
                            .map(AdminEventsByPeriodPointDto::eventsCount)
                            .filter(Objects::nonNull)
                            .max(Long::compareTo)
                            .orElse(0L)
            );
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("timelineErrorMessage", toTimelineLoadErrorMessage(ex));
        }

        model.addAttribute(
                "reportWindowFrom",
                overview != null && overview.from() != null
                        ? overview.from()
                        : eventsByPeriod != null ? eventsByPeriod.from() : null
        );
        model.addAttribute(
                "reportWindowTo",
                overview != null && overview.to() != null
                        ? overview.to()
                        : eventsByPeriod != null ? eventsByPeriod.to() : null
        );

        return "admin/reports";
    }

    private String toOverviewLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the report date filters and try again.";
            default -> "Unable to load overview metrics right now. Please try again.";
        };
    }

    private String toTimelineLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the timeline filters and try again.";
            default -> "Unable to load events-by-period data right now. Please try again.";
        };
    }
}
