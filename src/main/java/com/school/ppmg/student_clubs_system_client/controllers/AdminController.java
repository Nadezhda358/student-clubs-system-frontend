package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {
    private final MembershipApplicationClient membershipApplicationClient;

    @GetMapping("/admin/events")
    public String adminEvents(Model model) {
        return placeholder(
                model,
                "Events",
                "Manage admin event workflows from one place.",
                "Event administration screens are being added here."
        );
    }

    @GetMapping({"/admin/applications/clubs", "/admin/membership-applications"})
    public String adminClubMembershipApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            Model model
    ) {
        List<MembershipApplicationDto> applications =
                membershipApplicationClient.adminGetAll(status, clubId, normalizeQuery(q));

        model.addAttribute("applications", applications == null ? Collections.emptyList() : applications);
        model.addAttribute("status", status);
        model.addAttribute("clubId", clubId);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("statusValues", MembershipRequestStatus.values());
        return "admin/membership-applications";
    }

    @GetMapping({"/admin/applications/events", "/admin/event-applications"})
    public String adminEventApplications(Model model) {
        return placeholder(
                model,
                "Event Applications",
                "Review participation requests and approvals for events.",
                "Event application management is coming soon."
        );
    }

    @GetMapping("/admin/stats")
    public String adminStats(Model model) {
        return placeholder(
                model,
                "Stats",
                "Track engagement and activity across clubs and events.",
                "Admin analytics dashboards are coming soon."
        );
    }

    private String placeholder(
            Model model,
            String pageTitle,
            String pageSubtitle,
            String comingSoonMessage
    ) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageSubtitle", pageSubtitle);
        model.addAttribute("comingSoonMessage", comingSoonMessage);
        return "admin/placeholder";
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String normalized = q.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
