package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Controller
@RequiredArgsConstructor
public class AdminController {
    private static final int PAGE_SIZE = 20;

    private final MembershipApplicationClient membershipApplicationClient;

    @GetMapping({"/admin/applications/clubs", "/admin/membership-applications"})
    public String adminClubMembershipApplications(
            @RequestParam(required = false) MembershipRequestStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("applications", Collections.emptyList());
        model.addAttribute("membershipPage", null);
        model.addAttribute("status", status);
        model.addAttribute("clubId", clubId);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("membershipPageBaseHref", buildMembershipPageBaseHref(status, clubId, q));
        model.addAttribute("statusValues", MembershipRequestStatus.values());
        model.addAttribute("pendingStatus", MembershipRequestStatus.PENDING);
        model.addAttribute("approvedStatusName", MembershipRequestStatus.APPROVED.name());
        model.addAttribute("rejectedStatusName", MembershipRequestStatus.REJECTED.name());
        model.addAttribute("membershipWorkspaceLabel", "Admin Workspace");
        model.addAttribute("membershipPageTitle", "Club Membership Applications");
        model.addAttribute(
                "membershipPageSubtitle",
                "Review pending requests quickly, approve eligible students, and keep membership decisions consistent."
        );
        model.addAttribute("membershipFilterAction", "/admin/membership-applications");
        model.addAttribute("membershipResetAction", "/admin/membership-applications");
        model.addAttribute("membershipActionBasePath", "/admin/membership-applications");
        model.addAttribute("membershipEmptyMessage", "No membership applications found.");

        try {
            PageResponse<MembershipApplicationDto> result = membershipApplicationClient.adminGetAll(
                    status,
                    clubId,
                    normalizeQuery(q),
                    page,
                    PAGE_SIZE,
                    "createdAt,desc"
            );
            model.addAttribute("membershipPage", result);
            model.addAttribute("applications", result.getContent() == null ? Collections.emptyList() : result.getContent());
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("loadErrorMessage", toMembershipApplicationLoadErrorMessage(ex));
        }

        return "admin/membership-applications";
    }

    @PostMapping("/admin/membership-applications/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveMembershipApplication(@PathVariable Long id) {
        return updateMembershipApplication(id, membershipApplicationClient::adminApprove);
    }

    @PostMapping("/admin/membership-applications/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectMembershipApplication(@PathVariable Long id) {
        return updateMembershipApplication(id, membershipApplicationClient::adminReject);
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String normalized = q.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildMembershipPageBaseHref(MembershipRequestStatus status, Long clubId, String q) {
        StringBuilder href = new StringBuilder("/admin/membership-applications?");
        if (status != null) {
            href.append("status=").append(status.name()).append("&");
        }
        if (clubId != null) {
            href.append("clubId=").append(clubId).append("&");
        }

        String normalizedQuery = normalizeQuery(q);
        if (normalizedQuery != null) {
            href.append("q=").append(URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8)).append("&");
        }

        return href.toString();
    }

    private ResponseEntity<?> updateMembershipApplication(Long id, Consumer<Long> action) {
        try {
            action.accept(id);
            return ResponseEntity.ok().build();
        } catch (FeignException ex) {
            HttpStatus status = resolveStatus(ex);
            return ResponseEntity
                    .status(status)
                    .body(Map.of("message", toMembershipApplicationUpdateErrorMessage(ex)));
        }
    }

    private HttpStatus resolveStatus(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        return status == null ? HttpStatus.BAD_GATEWAY : status;
    }

    private String toMembershipApplicationLoadErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "Membership applications endpoint is not available.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the filters and try again.";
            case FORBIDDEN -> "You are not authorized to view membership applications.";
            default -> "Unable to load membership applications right now. Please try again.";
        };
    }

    private String toMembershipApplicationUpdateErrorMessage(FeignException ex) {
        String extracted = extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (resolveStatus(ex)) {
            case NOT_FOUND -> "This membership application no longer exists.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Unable to update this membership application. Please refresh and try again.";
            case FORBIDDEN -> "You are not authorized to update this membership application.";
            case CONFLICT -> "This membership application has already been updated.";
            default -> "Unable to update membership application.";
        };
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
