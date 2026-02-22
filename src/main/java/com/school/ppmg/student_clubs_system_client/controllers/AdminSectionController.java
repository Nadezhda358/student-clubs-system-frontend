package com.school.ppmg.student_clubs_system_client.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSectionController {

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
    public String adminClubMembershipApplications(Model model) {
        return placeholder(
                model,
                "Club Membership Applications",
                "Review and process student requests to join clubs.",
                "Membership application moderation is coming soon."
        );
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
}
