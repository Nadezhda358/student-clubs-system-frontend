package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.MediaDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ClubController {
    private final ClubClient clubClient;
    private static final int PAGE_SIZE = 9;

    @GetMapping("/clubs")
    public String clubsPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String sort,
            Model model
    ) {
        // default sort if you want
        if (sort == null || sort.isBlank()) {
            sort = "name,asc";
        }

        PageResponse<ClubListDto> result = clubClient.getAll(active, page, PAGE_SIZE, sort);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());

        // keep query params for pagination links
        model.addAttribute("active", active);
        model.addAttribute("sort", sort);
        model.addAttribute("size", PAGE_SIZE);

        return "clubs/index";
    }

    @GetMapping("/clubs/{id}")
    public String clubDetails(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            ClubDto club = clubClient.getById(id);
            model.addAttribute("club", club);
            return "clubs/details";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        }
    }

    @GetMapping("/clubs/{id}/tabs/events")
    public String clubEventsTab(@PathVariable Long id, Model model) {
        try {
            List<Map<String, Object>> events = clubClient.getEvents(id);
            model.addAttribute("events", events);
        } catch (Exception ex) {
            model.addAttribute("error", "Events are not available yet.");
        }

        return "clubs/tabs/events :: content";
    }

    @GetMapping("/clubs/{id}/tabs/media")
    public String clubMediaTab(@PathVariable Long id, Model model) {
        try {
            List<MediaDto> media = clubClient.getMedia(id);
            model.addAttribute("media", media);
        } catch (Exception ex) {
            model.addAttribute("error", "Media is not available yet.");
        }

        return "clubs/tabs/media :: content";
    }

    @GetMapping("/clubs/{id}/tabs/announcements")
    public String clubAnnouncementsTab(@PathVariable Long id, Model model) {
        try {
            List<Map<String, Object>> announcements = clubClient.getAnnouncements(id);
            model.addAttribute("announcements", announcements);
        } catch (Exception ex) {
            model.addAttribute("error", "Announcements are not available yet.");
        }

        return "clubs/tabs/announcements :: content";
    }
}
