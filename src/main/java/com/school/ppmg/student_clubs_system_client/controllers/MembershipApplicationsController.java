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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/me/membership-applications/{id}/cancel")
    public String cancelMyMembershipApplication(
            @PathVariable Long id,
            @RequestParam(required = false) MembershipRequestStatus status,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            RedirectAttributes redirectAttributes
    ) {
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Please sign in to manage your membership applications."
            );
            return "redirect:/login";
        }

        if (sessionUser.role() != UserRole.STUDENT) {
            redirectAttributes.addFlashAttribute(
                    "membershipActionWarningMessage",
                    "Only students can cancel membership applications."
            );
            return redirectToMembershipApplications(status);
        }

        try {
            membershipApplicationClient.cancelMyApplication(id);
            redirectAttributes.addFlashAttribute("membershipActionSuccessMessage", "Application cancelled.");
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Please sign in to manage your membership applications."
                );
                return "redirect:/login";
            }

            if (resolveStatus(ex) == HttpStatus.FORBIDDEN) {
                redirectAttributes.addFlashAttribute(
                        "membershipActionWarningMessage",
                        firstNonBlank(extractUserMessage(ex), "You are not allowed to cancel this application.")
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "membershipActionErrorMessage",
                        toCancelErrorMessage(ex)
                );
            }
        }

        return redirectToMembershipApplications(status);
    }

    private String toListLoadErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "Membership applications endpoint is not available.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Invalid status filter. Please choose a valid option.";
            case UNAUTHORIZED -> "Please sign in to view your applications.";
            case FORBIDDEN -> "Only students can view membership applications.";
            default -> "Unable to load your applications right now. Please try again.";
        };
    }

    private String toCancelErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "This membership application was not found.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "This application can no longer be cancelled.";
            case CONFLICT -> "This membership application has already been updated.";
            default -> "Unable to cancel this membership application right now. Please try again.";
        };
    }

    private String redirectToMembershipApplications(MembershipRequestStatus status) {
        if (status == null) {
            return "redirect:/me/membership-applications";
        }

        return "redirect:/me/membership-applications?status=" + status.name();
    }

    private HttpStatus resolveStatus(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        return status == null ? HttpStatus.BAD_GATEWAY : status;
    }

    private String extractUserMessage(FeignException ex) {
        String content = ex.contentUTF8();
        if (content == null || content.isBlank()) {
            return "";
        }

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

        String compact = content.trim();
        if (!compact.startsWith("<") && compact.length() <= 220) {
            return compact;
        }

        return "";
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
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
