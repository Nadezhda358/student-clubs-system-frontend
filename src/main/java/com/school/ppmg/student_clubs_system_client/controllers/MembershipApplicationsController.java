package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import com.school.ppmg.student_clubs_system_client.enums.UserRole;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MembershipApplicationsController {
    private final MembershipApplicationClient membershipApplicationClient;

    @GetMapping("/me/membership-applications")
    public String myMembershipApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            Model model
    ) {
        model.addAttribute("selectedStatus", status);
        model.addAttribute("applications", Collections.emptyList());

        if (sessionUser == null) {
            return "redirect:/login";
        }

        if (sessionUser.role() != UserRole.STUDENT) {
            model.addAttribute("accessMessage", "Only students can view membership applications.");
            return "me/membership-applications";
        }

        try {
            List<MembershipApplicationDto> applications = membershipApplicationClient.getMyApplications(status);
            model.addAttribute("applications", applications == null ? Collections.emptyList() : applications);
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("accessMessage", "Only students can view membership applications.");
                return "me/membership-applications";
            }

            model.addAttribute("loadErrorMessage", toListLoadErrorMessage(ex));
        }

        return "me/membership-applications";
    }

    private String toListLoadErrorMessage(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String content = ex.contentUTF8();
        if (content != null && !content.isBlank()) {
            String extracted = extractJsonField(content, "message");
            if (!extracted.isBlank()) {
                return extracted;
            }

            extracted = extractJsonField(content, "error");
            if (!extracted.isBlank()) {
                return extracted;
            }

            extracted = extractJsonField(content, "detail");
            if (!extracted.isBlank()) {
                return extracted;
            }
        }

        return switch (status) {
            case NOT_FOUND -> "Membership applications endpoint is not available.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Invalid status filter. Please choose a valid option.";
            case UNAUTHORIZED -> "Please sign in to view your applications.";
            case FORBIDDEN -> "Only students can view membership applications.";
            default -> "Unable to load your applications right now. Please try again.";
        };
    }

    private String extractJsonField(String json, String fieldName) {
        String token = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(token);
        if (fieldIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', fieldIndex + token.length());
        if (colonIndex < 0) {
            return "";
        }

        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return "";
        }

        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }

        return json.substring(firstQuote + 1, secondQuote).trim();
    }
}
