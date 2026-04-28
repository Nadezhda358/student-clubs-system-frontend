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
    private static final int PAGE_SIZE = 10;

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
                    PAGE_SIZE,
                    null
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
                "Създай съобщение",
                "Публикувайте съобщение към клуб и изберете дали да бъде видимо веднага.",
                "Създай съобщение",
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
                    "Създай съобщение",
                    "Публикувайте съобщение към клуб и изберете дали да бъде видимо веднага.",
                    "Създай съобщение",
                    "/admin/announcements/create",
                    "/admin/announcements"
            );
            model.addAttribute("errorMessage", validationMessage);
            return "admin/announcement-form";
        }

        try {
            adminAnnouncementClient.createAdminAnnouncement(toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е създадено успешно.");
            return "redirect:/admin/announcements";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Създай съобщение",
                    "Публикувайте съобщение към клуб и изберете дали да бъде видимо веднага.",
                    "Създай съобщение",
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
                    "Редактирай съобщение",
                    "Обновете клубното съобщение и неговата видимост за учениците.",
                    "Запази промените",
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
                    "Редактирай съобщение",
                    "Обновете клубното съобщение и неговата видимост за учениците.",
                    "Запази промените",
                    "/admin/announcements/" + id + "/edit",
                    "/admin/announcements"
            );
            model.addAttribute("announcementId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "admin/announcement-form";
        }

        try {
            adminAnnouncementClient.updateAdminAnnouncement(id, toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е обновено успешно.");
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
                    "Редактирай съобщение",
                    "Обновете клубното съобщение и неговата видимост за учениците.",
                    "Запази промените",
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
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е изтрито успешно.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:/admin/announcements";
    }

    private List<ClubListDto> loadClubs() {
        try {
            PageResponse<ClubListDto> response = clubClient.getAll(null, null, 0, 200, null);
            return response.getContent() == null ? List.of() : response.getContent();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private void populateFormModel(
            Model model,
            AnnouncementFormRequest form,
            List<ClubListDto> clubOptions,
            String pageTitle,
            String pageSubtitle,
            String submitLabel,
            String formAction,
            String cancelHref
    ) {
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
            return "Изберете клуб.";
        }

        if (EventViewSupport.trimToNull(form.getTitle()) == null) {
            return "Заглавието на съобщението е задължително.";
        }

        if (EventViewSupport.trimToNull(form.getBody()) == null) {
            return "Текстът на съобщението е задължителен.";
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
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Прегледайте филтрите и опитайте отново.";
            default -> "Съобщенията не могат да се заредят в момента. Опитайте отново.";
        };
    }

    private String toAnnouncementSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> creating
                    ? "Прегледайте данните за новото съобщение и опитайте отново."
                    : "Прегледайте обновените данни за съобщението и опитайте отново.";
            case NOT_FOUND -> "Избраният клуб или съобщение вече не е налично.";
            default -> creating
                    ? "Съобщението не може да бъде създадено в момента. Опитайте отново."
                    : "Съобщението не може да бъде запазено в момента. Опитайте отново.";
        };
    }

    private String toDeleteErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case NOT_FOUND -> "Това съобщение вече не съществува.";
            default -> "Съобщението не може да бъде изтрито в момента. Опитайте отново.";
        };
    }
}
