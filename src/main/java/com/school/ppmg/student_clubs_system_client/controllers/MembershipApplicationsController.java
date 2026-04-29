package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
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
    private static final int PAGE_SIZE = 12;

    private final MembershipApplicationClient membershipApplicationClient;

    @GetMapping("/me/membership-applications")
    public String myMembershipApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            Model model
    ) {
        model.addAttribute("selectedStatus", status);
        model.addAttribute("applications", Collections.emptyList());
        model.addAttribute("membershipPage", null);
        model.addAttribute("membershipPageBaseHref", buildMembershipPageBaseHref(status));

        if (sessionUser == null) {
            return "redirect:/login";
        }

        if (sessionUser.role() != UserRole.STUDENT) {
            model.addAttribute("accessMessage", "Само ученици могат да виждат кандидатурите за членство.");
            return "me/membership-applications";
        }

        try {
            PageResponse<MembershipApplicationDto> result = membershipApplicationClient.getMyApplications(
                    status,
                    null,
                    null,
                    page,
                    PAGE_SIZE,
                    null
            );
            model.addAttribute("membershipPage", result);
            model.addAttribute(
                    "applications",
                    result.getContent() == null ? Collections.emptyList() : result.getContent()
            );
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("accessMessage", "Само ученици могат да виждат кандидатурите за членство.");
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
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            RedirectAttributes redirectAttributes
    ) {
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Влезте, за да управлявате кандидатурите си за членство."
            );
            return "redirect:/login";
        }

        if (sessionUser.role() != UserRole.STUDENT) {
            redirectAttributes.addFlashAttribute(
                    "membershipActionWarningMessage",
                    "Само ученици могат да отменят кандидатури за членство."
            );
            return redirectToMembershipApplications(status, page);
        }

        try {
            membershipApplicationClient.cancelMyApplication(id);
            redirectAttributes.addFlashAttribute("membershipActionSuccessMessage", "Кандидатурата е отменена.");
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Влезте, за да управлявате кандидатурите си за членство."
                );
                return "redirect:/login";
            }

            if (resolveStatus(ex) == HttpStatus.FORBIDDEN) {
                redirectAttributes.addFlashAttribute(
                        "membershipActionWarningMessage",
                        firstNonBlank(extractUserMessage(ex), "Нямате право да отменяте тази кандидатура.")
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "membershipActionErrorMessage",
                        toCancelErrorMessage(ex)
                );
            }
        }

        return redirectToMembershipApplications(status, page);
    }

    private String toListLoadErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "Кандидатурите за членство не са налични в момента.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Невалиден филтър за статус. Изберете валидна опция.";
            case UNAUTHORIZED -> "Влезте, за да видите кандидатурите си.";
            case FORBIDDEN -> "Само ученици могат да виждат кандидатурите за членство.";
            default -> "Вашите кандидатури не могат да се заредят в момента. Опитайте отново.";
        };
    }

    private String toCancelErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "Тази кандидатура за членство не беше намерена.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Тази кандидатура вече не може да бъде отменена.";
            case CONFLICT -> "Тази кандидатура за членство вече е обновена.";
            default -> "Кандидатурата за членство не може да бъде отменена в момента. Опитайте отново.";
        };
    }

    private String redirectToMembershipApplications(MembershipRequestStatus status, int page) {
        return "redirect:" + buildMembershipPageBaseHref(status) + "page=" + page;
    }

    private String buildMembershipPageBaseHref(MembershipRequestStatus status) {
        StringBuilder href = new StringBuilder("/me/membership-applications?");
        if (status != null) {
            href.append("status=").append(status.name()).append("&");
        }
        return href.toString();
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
