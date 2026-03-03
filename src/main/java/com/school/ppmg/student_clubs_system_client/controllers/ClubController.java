package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.CreateMembershipApplicationRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.MediaDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpsertClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import com.school.ppmg.student_clubs_system_client.enums.UserRole;
import feign.FeignException;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ClubController {
    private final ClubClient clubClient;
    private final MembershipApplicationClient membershipApplicationClient;
    private static final int PAGE_SIZE = 9;

    @GetMapping("/clubs")
    public String clubsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String sort,
            Model model
    ) {
        // default sort if you want
        if (sort == null || sort.isBlank()) {
            sort = "name,asc";
        }

        PageResponse<ClubListDto> result = clubClient.getAll(true, page, PAGE_SIZE, sort);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());

        // keep query params for pagination links
        model.addAttribute("sort", sort);
        model.addAttribute("size", PAGE_SIZE);

        return "clubs/index";
    }

    @GetMapping("/admin/clubs")
    public String adminClubsPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String success,
            Model model
    ) {
        String resolvedSort = (sort == null || sort.isBlank()) ? "name,asc" : sort;
        PageResponse<ClubListDto> result = clubClient.getAll(active, page, PAGE_SIZE, resolvedSort);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());
        model.addAttribute("active", active);
        model.addAttribute("sort", resolvedSort);
        model.addAttribute("size", PAGE_SIZE);
        model.addAttribute("successMessage", successMessage(success));

        return "admin/clubs";
    }

    @GetMapping("/admin/clubs/create")
    public String createClubPage(Model model) {
        populateFormModel(
                model,
                "create",
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                true
        );
        return "admin/club-form";
    }

    @PostMapping("/admin/clubs/create")
    public String createClub(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String scheduleText,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(defaultValue = "false") boolean isActive,
            Model model
    ) {
        String normalizedName = normalizeRequiredText(name);
        String normalizedDescription = normalizeOptionalText(description);
        String normalizedScheduleText = normalizeOptionalText(scheduleText);
        String normalizedRoom = normalizeOptionalText(room);
        String normalizedContactEmail = normalizeOptionalText(contactEmail);
        String normalizedContactPhone = normalizeOptionalText(contactPhone);

        if (normalizedName.isBlank()) {
            populateFormModel(
                    model,
                    "create",
                    null,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            );
            model.addAttribute("errorMessage", "Club Name is required.");
            return "admin/club-form";
        }

        try {
            clubClient.create(new UpsertClubDto(
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            ));
            return "redirect:/admin/clubs?success=created";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    "create",
                    null,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            );
            model.addAttribute("errorMessage", toClubSaveErrorMessage(ex));
            return "admin/club-form";
        }
    }

    @GetMapping("/admin/clubs/{id}/edit")
    public String editClubPage(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            ClubDto club = clubClient.getById(id);
            populateFormModel(
                    model,
                    "edit",
                    id,
                    nonNull(club.name()),
                    nonNull(club.description()),
                    nonNull(club.scheduleText()),
                    nonNull(club.room()),
                    nonNull(club.contactEmail()),
                    nonNull(club.contactPhone()),
                    club.isActive() == null || club.isActive()
            );
            return "admin/club-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("not found")) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                model.addAttribute("missingClubId", id);
                return "errors/404";
            }
            throw ex;
        }
    }

    @PostMapping("/admin/clubs/{id}/edit")
    public String updateClub(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String scheduleText,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(defaultValue = "false") boolean isActive,
            Model model,
            HttpServletResponse response
    ) {
        String normalizedName = normalizeRequiredText(name);
        String normalizedDescription = normalizeOptionalText(description);
        String normalizedScheduleText = normalizeOptionalText(scheduleText);
        String normalizedRoom = normalizeOptionalText(room);
        String normalizedContactEmail = normalizeOptionalText(contactEmail);
        String normalizedContactPhone = normalizeOptionalText(contactPhone);

        if (normalizedName.isBlank()) {
            populateFormModel(
                    model,
                    "edit",
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            );
            model.addAttribute("errorMessage", "Club Name is required.");
            return "admin/club-form";
        }

        try {
            clubClient.update(id, new UpsertClubDto(
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            ));
            return "redirect:/admin/clubs?success=updated";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    "edit",
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            );
            model.addAttribute("errorMessage", toClubSaveErrorMessage(ex));
            return "admin/club-form";
        }
    }

    @GetMapping("/clubs/{id}")
    public String clubDetails(
            @PathVariable Long id,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            Model model,
            HttpServletResponse response
    ) {
        try {
            ClubDto club = clubClient.getById(id);
            model.addAttribute("club", club);

            boolean hasPendingApplication = false;
            if (isStudent(sessionUser)) {
                try {
                    hasPendingApplication = membershipApplicationClient
                            .getMyApplications(MembershipRequestStatus.PENDING)
                            .stream()
                            .anyMatch(application -> application.clubId() != null && application.clubId().equals(id));
                } catch (RuntimeException ignored) {
                    // Club page should still render even if membership status lookup fails.
                }
            }

            model.addAttribute("membershipApplicationPending", hasPendingApplication);
            return "clubs/details";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        }
    }

    @PostMapping("/clubs/{id}/membership-applications/apply")
    public String applyForMembership(
            @PathVariable("id") Long clubId,
            @RequestParam(required = false) String motivationText,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            RedirectAttributes redirectAttributes
    ) {
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("success", "Please sign in to apply for club membership.");
            return "redirect:/login";
        }

        if (!isStudent(sessionUser)) {
            redirectAttributes.addFlashAttribute("membershipApplyWarningMessage", "Only students can apply.");
            return "redirect:/clubs/" + clubId;
        }

        String normalizedMotivation = normalizeMembershipMotivation(motivationText);
        if (normalizedMotivation != null && normalizedMotivation.length() > 2000) {
            redirectAttributes.addFlashAttribute(
                    "membershipApplyErrorMessage",
                    "Motivation text must be 2000 characters or fewer."
            );
            redirectAttributes.addFlashAttribute("membershipApplyDraft", normalizedMotivation);
            return "redirect:/clubs/" + clubId;
        }

        try {
            membershipApplicationClient.apply(clubId, new CreateMembershipApplicationRequest(normalizedMotivation));
            redirectAttributes.addFlashAttribute("membershipApplySuccessMessage", "Application submitted.");
            redirectAttributes.addFlashAttribute("membershipApplicationSubmitted", true);
            return "redirect:/clubs/" + clubId;
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                redirectAttributes.addFlashAttribute("success", "Please sign in to apply for club membership.");
                return "redirect:/login";
            }

            redirectAttributes.addFlashAttribute("membershipApplyDraft", normalizedMotivation == null ? "" : normalizedMotivation);
            addMembershipApplyErrorFlash(redirectAttributes, ex);
            return "redirect:/clubs/" + clubId;
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

    private void populateFormModel(
            Model model,
            String mode,
            Long clubId,
            String name,
            String description,
            String scheduleText,
            String room,
            String contactEmail,
            String contactPhone,
            boolean isActive
    ) {
        boolean isEdit = "edit".equalsIgnoreCase(mode);
        model.addAttribute("mode", isEdit ? "edit" : "create");
        model.addAttribute("clubId", clubId);
        model.addAttribute("clubName", nonNull(name));
        model.addAttribute("clubDescription", nonNull(description));
        model.addAttribute("clubScheduleText", nonNull(scheduleText));
        model.addAttribute("clubRoom", nonNull(room));
        model.addAttribute("clubContactEmail", nonNull(contactEmail));
        model.addAttribute("clubContactPhone", nonNull(contactPhone));
        model.addAttribute("clubIsActive", isActive);
        model.addAttribute("pageTitle", isEdit ? "Edit Club" : "Create Club");
        model.addAttribute("pageSubtitle", isEdit
                ? "Update club details and keep information current."
                : "Add a new club with schedule and contact details.");
        model.addAttribute("submitLabel", isEdit ? "Save Changes" : "Create Club");
    }

    private String successMessage(String success) {
        if (success == null || success.isBlank()) {
            return null;
        }

        if ("created".equalsIgnoreCase(success)) {
            return "Club created successfully.";
        }

        if ("updated".equalsIgnoreCase(success)) {
            return "Club updated successfully.";
        }

        return null;
    }

    private String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeRequiredText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeMembershipMotivation(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isStudent(AuthUserDto sessionUser) {
        return sessionUser != null && sessionUser.role() == UserRole.STUDENT;
    }

    private void addMembershipApplyErrorFlash(RedirectAttributes redirectAttributes, FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String message = switch (status) {
            case CONFLICT -> "You already have a pending application for this club.";
            case FORBIDDEN -> "Only students can apply.";
            case NOT_FOUND -> "This club was not found.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> firstNonBlank(
                    extractUserMessage(ex),
                    "Please review your motivation text and try again."
            );
            default -> firstNonBlank(
                    extractUserMessage(ex),
                    "Unable to submit your application right now. Please try again."
            );
        };

        if (status == HttpStatus.CONFLICT || status == HttpStatus.FORBIDDEN) {
            redirectAttributes.addFlashAttribute("membershipApplyWarningMessage", message);
            return;
        }

        redirectAttributes.addFlashAttribute("membershipApplyErrorMessage", message);
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

    private String toClubSaveErrorMessage(FeignException ex) {
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

            String compact = content.trim();
            if (!compact.startsWith("<") && compact.length() <= 220) {
                return compact;
            }
        }

        return switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the club details and try again.";
            case UNAUTHORIZED, FORBIDDEN -> "You are not authorized to perform this action.";
            case NOT_FOUND -> "Club not found.";
            default -> "Unable to save the club right now. Please try again.";
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

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
