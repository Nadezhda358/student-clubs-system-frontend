package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.TeacherAnnouncementClient;
import com.school.ppmg.student_clubs_system_client.clients.TeacherClubClient;
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
public class TeacherAnnouncementController {
    private static final int PAGE_SIZE = 10;

    private final TeacherAnnouncementClient teacherAnnouncementClient;
    private final TeacherClubClient teacherClubClient;

    @GetMapping("/teacher/announcements")
    public String teacherAnnouncements(
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
        List<ClubListDto> clubOptions = loadManagedClubs();
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
        model.addAttribute("createHref", buildTeacherCreateHref(clubId));

        try {
            PageResponse<AnnouncementDto> result = teacherAnnouncementClient.getTeacherAnnouncements(
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

        return "teacher/announcements";
    }

    @GetMapping("/teacher/announcements/create")
    public String createTeacherAnnouncementPage(
            @RequestParam(required = false) Long clubId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        if (clubOptions.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Трябва да имате поне един управляван клуб, преди да създавате съобщения."
            );
            return "redirect:/teacher/announcements";
        }

        AnnouncementFormRequest form = new AnnouncementFormRequest();
        if (clubId != null) {
            form.setClubId(clubId);
        } else if (clubOptions.size() == 1) {
            form.setClubId(clubOptions.get(0).id());
        }

        populateFormModel(
                model,
                form,
                clubOptions,
                "Създай съобщение",
                "Публикувайте съобщение за един от клубовете, които управлявате.",
                "Създай съобщение",
                "/teacher/announcements/create",
                buildTeacherAnnouncementsHref(form.getClubId())
        );
        return "teacher/announcement-form";
    }

    @PostMapping("/teacher/announcements/create")
    public String createTeacherAnnouncement(
            @ModelAttribute("form") AnnouncementFormRequest form,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        String validationMessage = validateAnnouncementForm(form);
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Създай съобщение",
                    "Публикувайте съобщение за един от клубовете, които управлявате.",
                    "Създай съобщение",
                    "/teacher/announcements/create",
                    buildTeacherAnnouncementsHref(form.getClubId())
            );
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/announcement-form";
        }

        try {
            teacherAnnouncementClient.createTeacherAnnouncement(toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е създадено успешно.");
            return "redirect:" + buildTeacherAnnouncementsHref(form.getClubId());
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Създай съобщение",
                    "Публикувайте съобщение за един от клубовете, които управлявате.",
                    "Създай съобщение",
                    "/teacher/announcements/create",
                    buildTeacherAnnouncementsHref(form.getClubId())
            );
            model.addAttribute("errorMessage", toAnnouncementSaveErrorMessage(ex, true));
            return "teacher/announcement-form";
        }
    }

    @GetMapping("/teacher/announcements/{id}/edit")
    public String editTeacherAnnouncementPage(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            AnnouncementDto announcement = teacherAnnouncementClient.getTeacherAnnouncementById(id);
            populateFormModel(
                    model,
                    toFormRequest(announcement),
                    loadManagedClubs(),
                    "Редактирай съобщение",
                    "Обновете времето и видимостта на това клубно съобщение.",
                    "Запази промените",
                    "/teacher/announcements/" + id + "/edit",
                    buildTeacherAnnouncementsHref(announcement.clubId())
            );
            model.addAttribute("announcementId", id);
            return "teacher/announcement-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "announcement");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Можете да управлявате само съобщения за клубове, които са ви назначени."
                );
                return "redirect:/teacher/announcements";
            }
            throw ex;
        }
    }

    @PostMapping("/teacher/announcements/{id}/edit")
    public String updateTeacherAnnouncement(
            @PathVariable Long id,
            @ModelAttribute("form") AnnouncementFormRequest form,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        String validationMessage = validateAnnouncementForm(form);
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Редактирай съобщение",
                    "Обновете времето и видимостта на това клубно съобщение.",
                    "Запази промените",
                    "/teacher/announcements/" + id + "/edit",
                    buildTeacherAnnouncementsHref(form.getClubId())
            );
            model.addAttribute("announcementId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/announcement-form";
        }

        try {
            teacherAnnouncementClient.updateTeacherAnnouncement(id, toUpsertAnnouncementDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е обновено успешно.");
            return "redirect:" + buildTeacherAnnouncementsHref(form.getClubId());
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "announcement");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Можете да управлявате само съобщения за клубове, които са ви назначени."
                );
                return "redirect:/teacher/announcements";
            }

            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Редактирай съобщение",
                    "Обновете времето и видимостта на това клубно съобщение.",
                    "Запази промените",
                    "/teacher/announcements/" + id + "/edit",
                    buildTeacherAnnouncementsHref(form.getClubId())
            );
            model.addAttribute("announcementId", id);
            model.addAttribute("errorMessage", toAnnouncementSaveErrorMessage(ex, false));
            return "teacher/announcement-form";
        }
    }

    @PostMapping("/teacher/announcements/{id}/delete")
    public String deleteTeacherAnnouncement(
            @PathVariable Long id,
            @RequestParam(required = false) Long clubId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            teacherAnnouncementClient.deleteTeacherAnnouncement(id);
            redirectAttributes.addFlashAttribute("successMessage", "Съобщението е изтрито успешно.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:" + buildTeacherAnnouncementsHref(clubId);
    }

    private List<ClubListDto> loadManagedClubs() {
        try {
            PageResponse<ClubListDto> response = teacherClubClient.getManagedClubs(null, null, 0, 200, null);
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

    private String buildTeacherCreateHref(Long clubId) {
        return clubId == null ? "/teacher/announcements/create" : "/teacher/announcements/create?clubId=" + clubId;
    }

    private String buildTeacherAnnouncementsHref(Long clubId) {
        return clubId == null ? "/teacher/announcements" : "/teacher/announcements?clubId=" + clubId;
    }

    private String toAnnouncementsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да управлявате само съобщения за клубове, които са ви назначени.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Прегледайте филтрите и опитайте отново.";
            default -> "Вашите съобщения не могат да се заредят в момента. Опитайте отново.";
        };
    }

    private String toAnnouncementSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да управлявате само съобщения за клубове, които са ви назначени.";
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
            case FORBIDDEN -> "Можете да изтривате само съобщения за клубове, които са ви назначени.";
            case NOT_FOUND -> "Това съобщение вече не съществува.";
            default -> "Съобщението не може да бъде изтрито в момента. Опитайте отново.";
        };
    }
}
