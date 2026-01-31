package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}
