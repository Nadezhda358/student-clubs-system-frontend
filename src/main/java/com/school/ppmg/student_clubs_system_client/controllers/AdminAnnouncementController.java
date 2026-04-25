package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.AdminAnnouncementClient;
import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.announcement.AnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.announcement.AnnouncementFormRequest;
import com.school.ppmg.student_clubs_system_client.dtos.announcement.UpsertAnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
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

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminAnnouncementController {
    private final AdminAnnouncementClient adminAnnouncementClient;
    private final ClubClient clubClient;

    @GetMapping("/admin/announcements")
    public String adminAnnouncements(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage,
            Model model
    ) {
        List<ClubListDto> clubOptions = loadClubs();
        model.addAttribute("clubOptions", clubOptions);
        model.addAttribute("announcements", Collections.emptyList());
        model.addAttribute("announcementPage", null);
        model.addAttribute("selectedClubId", clubId);
        model.addAttribute("selectedPublished", published);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate.trim());
        model.addAttribute("toDate", toDate == null ? "" : toDate.trim());
        model.addAttribute("successMessage", EventViewSupport.trimToNull(successMessage));
        model.addAttribute("errorMessage", EventViewSupport.trimToNull(errorMessage));

        try {
            PageResponse<AnnouncementDto> result = adminAnnouncementClient.getAdminAnnouncements(
                    clubId,
                    published,
                    EventViewSupport.trimToNull(q),
                    EventViewSupport.parseFromDate(fromDate),
                    EventViewSupport.parseToDate(toDate),
                    page,
                    EventViewSupport.BROWSER_PAGE_SIZE,
                    "publishedAt,desc"
            );
            model.addAttribute("announcementPage", result);
            model.addAttribute(
                    "announcements",
                    result.getContent() == null ? Collections.emptyList() : result.getContent()
            );
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("errorMessage", toAnnouncementsLoadErrorMessage(ex));
        }

        return "admin/announcements";
    }

    @GetMapping("/admin/announcements/create")
    public String createAdminAnnouncementPage(Model model) {
        populateFormModel(
                model,
                new AnnouncementFormRequest(),
                loadClubs(),
                "Admin Workspace",
                "Create Announcement",
                "Publish a message to a club and control whether it is visible immediately.",
                "Create Announcement",
                "/admin/announcements/create",
                "/admin/announcements"
        );
        return "admin/announcement-form";
    }

    @PostMapping("/admin/announcements/create")
    public String createAdminAnnouncement(
            @ModelAttribute("form") AnnouncementFormRequest form,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadClubs();
        String validationMessage = validateAnnouncementForm(form);
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Create Announcement",
                    "Publish a message to a club and control whether it is visible immediately.",
                    "Create Announcement",
                    "/admin/announcements/create",
                    "/admin/announcements"
            );
            model.addAttribute("errorMessage", validationMessage);
            return "admin/announcement-form";
        }

        try {
            adminAnnouncementClient.createAdminAnnouncement(toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Announcement created successfully.");
            return "redirect:/admin/announcements";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Create Announcement",
                    "Publish a message to a club and control whether it is visible immediately.",
                    "Create Announcement",
                    "/admin/announcements/create",
                    "/admin/announcements"
            );
            model.addAttribute("errorMessage", toAnnouncementSaveErrorMessage(ex, true));
            return "admin/announcement-form";
        }
    }

    @GetMapping("/admin/announcements/{id}/edit")
    public String editAdminAnnouncementPage(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            AnnouncementDto announcement = adminAnnouncementClient.getAdminAnnouncementById(id);
            populateFormModel(
                    model,
                    toFormRequest(announcement),
                    loadClubs(),
                    "Admin Workspace",
                    "Edit Announcement",
                    "Update club communication and control whether it remains visible to students.",
                    "Save Changes",
                    "/admin/announcements/" + id + "/edit",
                    "/admin/announcements"
            );
            model.addAttribute("announcementId", id);
            return "admin/announcement-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "announcement");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        }
    }

    @PostMapping("/admin/announcements/{id}/edit")
    public String updateAdminAnnouncement(
            @PathVariable Long id,
            @ModelAttribute("form") AnnouncementFormRequest form,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadClubs();
        String validationMessage = validateAnnouncementForm(form);
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Edit Announcement",
                    "Update club communication and control whether it remains visible to students.",
                    "Save Changes",
                    "/admin/announcements/" + id + "/edit",
                    "/admin/announcements"
            );
            model.addAttribute("announcementId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "admin/announcement-form";
        }

        try {
            adminAnnouncementClient.updateAdminAnnouncement(id, toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Announcement updated successfully.");
            return "redirect:/admin/announcements";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "announcement");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Edit Announcement",
                    "Update club communication and control whether it remains visible to students.",
                    "Save Changes",
                    "/admin/announcements/" + id + "/edit",
                    "/admin/announcements"
            );
            model.addAttribute("announcementId", id);
            model.addAttribute("errorMessage", toAnnouncementSaveErrorMessage(ex, false));
            return "admin/announcement-form";
        }
    }

    @PostMapping("/admin/announcements/{id}/delete")
    public String deleteAdminAnnouncement(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminAnnouncementClient.deleteAdminAnnouncement(id);
            redirectAttributes.addFlashAttribute("successMessage", "Announcement deleted successfully.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:/admin/announcements";
    }

    private List<ClubListDto> loadClubs() {
        try {
            PageResponse<ClubListDto> response = clubClient.getAll(null, null, 0, 200, "name,asc");
            return response.getContent() == null ? List.of() : response.getContent();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private void populateFormModel(
            Model model,
            AnnouncementFormRequest form,
            List<ClubListDto> clubOptions,
            String workspaceLabel,
            String pageTitle,
            String pageSubtitle,
            String submitLabel,
            String formAction,
            String cancelHref
    ) {
        model.addAttribute("workspaceLabel", workspaceLabel);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageSubtitle", pageSubtitle);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelHref", cancelHref);
        model.addAttribute("clubOptions", clubOptions);
        model.addAttribute("form", form);
    }

    private String validateAnnouncementForm(AnnouncementFormRequest form) {
        if (form.getClubId() == null) {
            return "Please choose a club.";
        }

        if (EventViewSupport.trimToNull(form.getTitle()) == null) {
            return "Announcement title is required.";
        }

        if (EventViewSupport.trimToNull(form.getBody()) == null) {
            return "Announcement body is required.";
        }

        return null;
    }

    private UpsertAnnouncementDto toUpsertAnnouncementDto(AnnouncementFormRequest form) {
        return new UpsertAnnouncementDto(
                form.getClubId(),
                EventViewSupport.trimToEmpty(form.getTitle()),
                EventViewSupport.trimToEmpty(form.getBody()),
                Boolean.TRUE.equals(form.getPublished())
        );
    }

    private AnnouncementFormRequest toFormRequest(AnnouncementDto announcement) {
        AnnouncementFormRequest form = new AnnouncementFormRequest();
        form.setClubId(announcement.clubId());
        form.setTitle(announcement.title() == null ? "" : announcement.title());
        form.setBody(announcement.body() == null ? "" : announcement.body());
        form.setPublished(Boolean.TRUE.equals(announcement.isPublished()));
        return form;
    }

    private String toAnnouncementsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the filters and try again.";
            default -> "Unable to load announcements right now. Please try again.";
        };
    }

    private String toAnnouncementSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> creating
                    ? "Please review the new announcement details and try again."
                    : "Please review the updated announcement details and try again.";
            case NOT_FOUND -> "The selected club or announcement is no longer available.";
            default -> creating
                    ? "Unable to create the announcement right now. Please try again."
                    : "Unable to save the announcement right now. Please try again.";
        };
    }

    private String toDeleteErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case NOT_FOUND -> "This announcement no longer exists.";
            default -> "Unable to delete the announcement right now. Please try again.";
        };
    }
}
