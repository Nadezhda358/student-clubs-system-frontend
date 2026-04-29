package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.TeacherClubClient;
import com.school.ppmg.student_clubs_system_client.clients.TeacherEventClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventFormRequest;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventListDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventParticipationDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.UpsertEventDto;
import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TeacherEventController {
    private static final long MAX_IMAGE_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int PAGE_SIZE = 10;
    private static final int PARTICIPANTS_PAGE_SIZE = 10;

    private final TeacherEventClient teacherEventClient;
    private final TeacherClubClient teacherClubClient;

    @GetMapping("/teacher/events")
    public String teacherEvents(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage,
            Model model
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();

        model.addAttribute("clubOptions", clubOptions);
        model.addAttribute("events", Collections.emptyList());
        model.addAttribute("eventPage", null);
        model.addAttribute("selectedClubId", clubId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate.trim());
        model.addAttribute("toDate", toDate == null ? "" : toDate.trim());
        model.addAttribute("statusValues", EventStatus.values());
        model.addAttribute("successMessage", EventViewSupport.trimToNull(successMessage));
        model.addAttribute("errorMessage", EventViewSupport.trimToNull(errorMessage));
        model.addAttribute("createHref", buildTeacherCreateHref(clubId));

        try {
            PageResponse<EventListDto> result = teacherEventClient.getTeacherEvents(
                    clubId,
                    EventViewSupport.trimToNull(q),
                    EventViewSupport.parseFromDate(fromDate),
                    EventViewSupport.parseToDate(toDate),
                    null,
                    status,
                    page,
                    PAGE_SIZE,
                    null
            );
            model.addAttribute("eventPage", result);
            model.addAttribute("events", result.getContent() == null ? Collections.emptyList() : result.getContent());
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("errorMessage", toEventsLoadErrorMessage(ex));
        }

        return "teacher/events";
    }

    @GetMapping("/teacher/events/create")
    public String createTeacherEventPage(
            @RequestParam(required = false) Long clubId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        if (clubOptions.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Трябва да имате поне един управляван клуб, преди да създавате събития.");
            return "redirect:/teacher/events";
        }

        EventFormRequest form = new EventFormRequest();
        if (clubId != null) {
            form.setClubId(clubId);
        } else if (clubOptions.size() == 1) {
            form.setClubId(clubOptions.get(0).id());
        }

        populateFormModel(
                model,
                form,
                clubOptions,
                "Създай събитие",
                "Планирайте ново събитие за един от клубовете, които управлявате.",
                "Създай събитие",
                "/teacher/events/create",
                buildTeacherEventsHref(form.getClubId()),
                ""
        );
        return "teacher/event-form";
    }

    @PostMapping("/teacher/events/create")
    public String createTeacherEvent(
            @ModelAttribute("form") EventFormRequest form,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        String validationMessage = validateEventForm(form);
        if (validationMessage == null) {
            validationMessage = validateMainImage(form.getMainImage());
        }

        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Създай събитие",
                    "Планирайте ново събитие за един от клубовете, които управлявате.",
                    "Създай събитие",
                    "/teacher/events/create",
                    buildTeacherEventsHref(form.getClubId()),
                    ""
            );
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/event-form";
        }

        try {
            EventDto createdEvent = teacherEventClient.createTeacherEvent(toUpsertEventDto(form));
            if (hasFile(form.getMainImage())) {
                try {
                    teacherEventClient.uploadTeacherEventMainImage(createdEvent.id(), form.getMainImage());
                } catch (FeignException ex) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            EventViewSupport.firstNonBlank(
                                    EventViewSupport.extractUserMessage(ex),
                                    "Събитието беше създадено, но качването на основното изображение не успя. Можете да опитате отново от страницата за редакция."
                            )
                    );
                    return "redirect:/teacher/events/" + createdEvent.id() + "/edit";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Събитието е създадено успешно.");
            return "redirect:" + buildTeacherEventsHref(form.getClubId());
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Създай събитие",
                    "Планирайте ново събитие за един от клубовете, които управлявате.",
                    "Създай събитие",
                    "/teacher/events/create",
                    buildTeacherEventsHref(form.getClubId()),
                    ""
            );
            model.addAttribute("errorMessage", toEventSaveErrorMessage(ex, true));
            return "teacher/event-form";
        }
    }

    @GetMapping("/teacher/events/{id}/edit")
    public String editTeacherEventPage(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            EventDto event = teacherEventClient.getTeacherEventById(id);
            EventFormRequest form = toFormRequest(event);
            populateFormModel(
                    model,
                    form,
                    loadManagedClubs(),
                    "Редактирай събитие",
                    "Променете графика, настройките за записване или капацитета на това събитие.",
                    "Запази промените",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(event.clubId()),
                    nonNull(event.mainImageUrl())
            );
            model.addAttribute("eventId", id);
            return "teacher/event-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Можете да управлявате само събития за клубове, които са ви назначени.");
                return "redirect:/teacher/events";
            }
            throw ex;
        }
    }

    @PostMapping("/teacher/events/{id}/edit")
    public String updateTeacherEvent(
            @PathVariable Long id,
            @ModelAttribute("form") EventFormRequest form,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadManagedClubs();
        String validationMessage = validateEventForm(form);
        if (validationMessage == null) {
            validationMessage = validateMainImage(form.getMainImage());
        }

        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Редактирай събитие",
                    "Променете графика, настройките за записване или капацитета на това събитие.",
                    "Запази промените",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(form.getClubId()),
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("eventId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/event-form";
        }

        try {
            teacherEventClient.updateTeacherEvent(id, toUpsertEventDto(form));
            if (hasFile(form.getMainImage())) {
                try {
                    teacherEventClient.uploadTeacherEventMainImage(id, form.getMainImage());
                } catch (FeignException ex) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            EventViewSupport.firstNonBlank(
                                    EventViewSupport.extractUserMessage(ex),
                                    "Данните за събитието бяха запазени, но качването на основното изображение не успя. Опитайте отново."
                            )
                    );
                    return "redirect:/teacher/events/" + id + "/edit";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Събитието е обновено успешно.");
            return "redirect:" + buildTeacherEventsHref(form.getClubId());
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Можете да управлявате само събития за клубове, които са ви назначени.");
                return "redirect:/teacher/events";
            }

            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Редактирай събитие",
                    "Променете графика, настройките за записване или капацитета на това събитие.",
                    "Запази промените",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(form.getClubId()),
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("eventId", id);
            model.addAttribute("errorMessage", toEventSaveErrorMessage(ex, false));
            return "teacher/event-form";
        }
    }

    @PostMapping("/teacher/events/{id}/delete")
    public String deleteTeacherEvent(
            @PathVariable Long id,
            @RequestParam(required = false) Long clubId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            teacherEventClient.deleteTeacherEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Събитието е изтрито успешно.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:" + buildTeacherEventsHref(clubId);
    }

    @GetMapping("/teacher/event-participations")
    public String teacherEventParticipations(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String registrationStatus,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage,
            Model model
    ) {
        RegistrationStatus selectedRegistrationStatus = EventViewSupport.parseRegistrationStatus(registrationStatus);

        model.addAttribute("clubOptions", loadManagedClubs());
        model.addAttribute("participations", Collections.emptyList());
        model.addAttribute("participationPage", null);
        model.addAttribute("selectedClubId", clubId);
        model.addAttribute("selectedRegistrationStatus", selectedRegistrationStatus);
        model.addAttribute("selectedEventStatus", eventStatus);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("registrationStatusValues", RegistrationStatus.values());
        model.addAttribute("eventStatusValues", EventStatus.values());
        model.addAttribute("successMessage", EventViewSupport.trimToNull(successMessage));
        model.addAttribute("errorMessage", EventViewSupport.trimToNull(errorMessage));

        try {
            PageResponse<EventParticipationDto> result = teacherEventClient.getTeacherParticipations(
                    clubId,
                    null,
                    selectedRegistrationStatus,
                    eventStatus,
                    EventViewSupport.trimToNull(q),
                    null,
                    page,
                    PARTICIPANTS_PAGE_SIZE,
                    null
            );
            model.addAttribute("participationPage", result);
            model.addAttribute("participations", result.getContent() == null ? Collections.emptyList() : result.getContent());
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                return "redirect:/login";
            }

            model.addAttribute("errorMessage", toParticipationLoadErrorMessage(ex));
        }

        return "teacher/event-participations";
    }

    @GetMapping("/teacher/events/{id}/participants")
    public String teacherEventParticipants(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        RegistrationStatus selectedStatus = EventViewSupport.parseRegistrationStatus(status);

        try {
            EventDto event = teacherEventClient.getTeacherEventById(id);
            model.addAttribute("event", event);
            model.addAttribute("participants", Collections.emptyList());
            model.addAttribute("participantPage", null);
            model.addAttribute("selectedStatus", selectedStatus);
            model.addAttribute("q", q == null ? "" : q.trim());
            model.addAttribute("statusValues", RegistrationStatus.values());
            model.addAttribute("successMessage", EventViewSupport.trimToNull(successMessage));
            model.addAttribute("errorMessage", EventViewSupport.trimToNull(errorMessage));

            PageResponse<EventParticipationDto> result = teacherEventClient.getTeacherParticipants(
                    id,
                    selectedStatus,
                    EventViewSupport.trimToNull(q),
                    page,
                    PARTICIPANTS_PAGE_SIZE,
                    null
            );
            model.addAttribute("participantPage", result);
            model.addAttribute("participants", result.getContent() == null ? Collections.emptyList() : result.getContent());
            return "teacher/event-participants";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Нямате право да виждате участниците в това събитие.");
                return "redirect:/teacher/events";
            }

            throw ex;
        }
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
            EventFormRequest form,
            List<ClubListDto> clubOptions,
            String pageTitle,
            String pageSubtitle,
            String submitLabel,
            String formAction,
            String cancelHref,
            String eventMainImageUrl
    ) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageSubtitle", pageSubtitle);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelHref", cancelHref);
        model.addAttribute("clubOptions", clubOptions);
        model.addAttribute("statusValues", EventStatus.values());
        model.addAttribute("audienceValues", EventAudience.values());
        model.addAttribute("eventMainImageUrl", nonNull(eventMainImageUrl));
        model.addAttribute("form", form);
    }

    private String validateEventForm(EventFormRequest form) {
        if (form.getClubId() == null) {
            return "Изберете клуб.";
        }

        if (EventViewSupport.trimToNull(form.getTitle()) == null) {
            return "Заглавието на събитието е задължително.";
        }

        if (EventViewSupport.trimToNull(form.getDescription()) == null) {
            return "Описанието е задължително.";
        }

        if (EventViewSupport.parseDateTimeInput(form.getStartAt()) == null) {
            return "Началната дата и час са задължителни.";
        }

        if (EventViewSupport.trimToNull(form.getEndAt()) != null && EventViewSupport.parseDateTimeInput(form.getEndAt()) == null) {
            return "Крайната дата и час трябва да са валидни.";
        }

        OffsetDateTime startAt = EventViewSupport.parseDateTimeInput(form.getStartAt());
        OffsetDateTime endAt = EventViewSupport.parseDateTimeInput(form.getEndAt());
        OffsetDateTime registrationDeadline = EventViewSupport.parseDateTimeInput(form.getRegistrationDeadline());

        if (endAt != null && startAt != null && endAt.isBefore(startAt)) {
            return "Крайната дата и час трябва да са след началото.";
        }

        if (registrationDeadline != null && startAt != null && registrationDeadline.isAfter(startAt)) {
            return "Крайният срок за записване трябва да е на или преди началото на събитието.";
        }

        if (form.getCapacity() != null && form.getCapacity() < 1) {
            return "Капацитетът трябва да е поне 1.";
        }

        if (form.getStatus() == null) {
            return "Изберете статус на събитието.";
        }

        if (form.getAudience() == null) {
            return "Изберете кой може да вижда събитието.";
        }

        return null;
    }

    private String validateMainImage(MultipartFile mainImage) {
        if (!hasFile(mainImage)) {
            return null;
        }

        if (mainImage.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            return "Основното изображение трябва да е 5 MB или по-малко. Изберете друг файл.";
        }

        if (!isImageFile(mainImage)) {
            return "Основното изображение трябва да е файл с изображение.";
        }

        return null;
    }

    private UpsertEventDto toUpsertEventDto(EventFormRequest form) {
        return new UpsertEventDto(
                form.getClubId(),
                EventViewSupport.trimToEmpty(form.getTitle()),
                EventViewSupport.trimToEmpty(form.getDescription()),
                EventViewSupport.parseDateTimeInput(form.getStartAt()),
                EventViewSupport.parseDateTimeInput(form.getEndAt()),
                EventViewSupport.trimToNull(form.getLocation()),
                form.getCapacity(),
                EventViewSupport.parseDateTimeInput(form.getRegistrationDeadline()),
                form.getStatus(),
                form.getAudience()
        );
    }

    private EventFormRequest toFormRequest(EventDto event) {
        EventFormRequest form = new EventFormRequest();
        form.setClubId(event.clubId());
        form.setTitle(event.title() == null ? "" : event.title());
        form.setDescription(event.description() == null ? "" : event.description());
        form.setStartAt(EventViewSupport.toDateTimeInput(event.startAt()));
        form.setEndAt(EventViewSupport.toDateTimeInput(event.endAt()));
        form.setLocation(event.location() == null ? "" : event.location());
        form.setCapacity(event.capacity());
        form.setRegistrationDeadline(EventViewSupport.toDateTimeInput(event.registrationDeadline()));
        form.setStatus(event.status());
        form.setAudience(event.audience());
        return form;
    }

    private String resolveCurrentMainImageUrl(Long id) {
        try {
            EventDto event = teacherEventClient.getTeacherEventById(id);
            return nonNull(event.mainImageUrl());
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String buildTeacherCreateHref(Long clubId) {
        return clubId == null ? "/teacher/events/create" : "/teacher/events/create?clubId=" + clubId;
    }

    private String buildTeacherEventsHref(Long clubId) {
        return clubId == null ? "/teacher/events" : "/teacher/events?clubId=" + clubId;
    }

    private String toEventsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да управлявате само събития за клубове, които са ви назначени.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Прегледайте филтрите и опитайте отново.";
            default -> "Вашите събития не могат да се заредят в момента. Опитайте отново.";
        };
    }

    private String toParticipationLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да виждате регистрации само за събития в клубове, които са ви назначени.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Прегледайте филтрите за участия и опитайте отново.";
            default -> "Регистрациите за събития не могат да се заредят в момента. Опитайте отново.";
        };
    }

    private String toEventSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да управлявате само събития за клубове, които са ви назначени.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> creating
                    ? "Прегледайте данните за новото събитие и опитайте отново."
                    : "Прегледайте обновените данни за събитието и опитайте отново.";
            case NOT_FOUND -> "Избраният клуб или събитие вече не е налично.";
            default -> creating
                    ? "Събитието не може да бъде създадено в момента. Опитайте отново."
                    : "Събитието не може да бъде запазено в момента. Опитайте отново.";
        };
    }

    private String toDeleteErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "Можете да изтривате само събития за клубове, които са ви назначени.";
            case NOT_FOUND -> "Това събитие вече не съществува.";
            default -> "Събитието не може да бъде изтрито в момента. Опитайте отново.";
        };
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private boolean isImageFile(MultipartFile file) {
        return hasFile(file)
                && file.getContentType() != null
                && file.getContentType().toLowerCase().startsWith("image/");
    }
}
