package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.AdminEventClient;
import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventFormRequest;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventListDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventParticipationDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.UpdateEventParticipationStatusRequest;
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
public class AdminEventController {
    private static final long MAX_IMAGE_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final AdminEventClient adminEventClient;
    private final ClubClient clubClient;

    @GetMapping("/admin/events")
    public String adminEvents(
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
        List<ClubListDto> clubOptions = loadClubs();
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

        try {
            PageResponse<EventListDto> result = adminEventClient.getAdminEvents(
                    clubId,
                    EventViewSupport.trimToNull(q),
                    EventViewSupport.parseFromDate(fromDate),
                    EventViewSupport.parseToDate(toDate),
                    null,
                    status,
                    page,
                    EventViewSupport.BROWSER_PAGE_SIZE,
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

        return "admin/events";
    }

    @GetMapping("/admin/events/create")
    public String createAdminEventPage(Model model) {
        EventFormRequest form = new EventFormRequest();
        populateFormModel(
                model,
                form,
                loadClubs(),
                "Admin Workspace",
                "Create Event",
                "Add a new event and assign it to the appropriate club.",
                "Create Event",
                "/admin/events/create",
                "/admin/events",
                ""
        );
        return "admin/event-form";
    }

    @PostMapping("/admin/events/create")
    public String createAdminEvent(
            @ModelAttribute("form") EventFormRequest form,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadClubs();
        String validationMessage = validateEventForm(form);
        if (validationMessage == null) {
            validationMessage = validateMainImage(form.getMainImage());
        }

        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Create Event",
                    "Add a new event and assign it to the appropriate club.",
                    "Create Event",
                    "/admin/events/create",
                    "/admin/events",
                    ""
            );
            model.addAttribute("errorMessage", validationMessage);
            return "admin/event-form";
        }

        try {
            EventDto createdEvent = adminEventClient.createAdminEvent(toUpsertEventDto(form));
            if (hasFile(form.getMainImage())) {
                try {
                    adminEventClient.uploadAdminEventMainImage(createdEvent.id(), form.getMainImage());
                } catch (FeignException ex) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            EventViewSupport.firstNonBlank(
                                    EventViewSupport.extractUserMessage(ex),
                                    "Event created, but the main image upload failed. You can try again from the edit page."
                            )
                    );
                    return "redirect:/admin/events/" + createdEvent.id() + "/edit";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Event created successfully.");
            return "redirect:/admin/events";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Create Event",
                    "Add a new event and assign it to the appropriate club.",
                    "Create Event",
                    "/admin/events/create",
                    "/admin/events",
                    ""
            );
            model.addAttribute("errorMessage", toEventSaveErrorMessage(ex, true));
            return "admin/event-form";
        }
    }

    @GetMapping("/admin/events/{id}/edit")
    public String editAdminEventPage(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            EventDto event = adminEventClient.getAdminEventById(id);
            populateFormModel(
                    model,
                    toFormRequest(event),
                    loadClubs(),
                    "Admin Workspace",
                    "Edit Event",
                    "Adjust event visibility, timing, or capacity across the platform.",
                    "Save Changes",
                    "/admin/events/" + id + "/edit",
                    "/admin/events",
                    nonNull(event.mainImageUrl())
            );
            model.addAttribute("eventId", id);
            return "admin/event-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        }
    }

    @PostMapping("/admin/events/{id}/edit")
    public String updateAdminEvent(
            @PathVariable Long id,
            @ModelAttribute("form") EventFormRequest form,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        List<ClubListDto> clubOptions = loadClubs();
        String validationMessage = validateEventForm(form);
        if (validationMessage == null) {
            validationMessage = validateMainImage(form.getMainImage());
        }

        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Edit Event",
                    "Adjust event visibility, timing, or capacity across the platform.",
                    "Save Changes",
                    "/admin/events/" + id + "/edit",
                    "/admin/events",
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("eventId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "admin/event-form";
        }

        try {
            adminEventClient.updateAdminEvent(id, toUpsertEventDto(form));
            if (hasFile(form.getMainImage())) {
                try {
                    adminEventClient.uploadAdminEventMainImage(id, form.getMainImage());
                } catch (FeignException ex) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            EventViewSupport.firstNonBlank(
                                    EventViewSupport.extractUserMessage(ex),
                                    "Event details were saved, but the main image upload failed. Please try again."
                            )
                    );
                    return "redirect:/admin/events/" + id + "/edit";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Event updated successfully.");
            return "redirect:/admin/events";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Admin Workspace",
                    "Edit Event",
                    "Adjust event visibility, timing, or capacity across the platform.",
                    "Save Changes",
                    "/admin/events/" + id + "/edit",
                    "/admin/events",
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("eventId", id);
            model.addAttribute("errorMessage", toEventSaveErrorMessage(ex, false));
            return "admin/event-form";
        }
    }

    @PostMapping("/admin/events/{id}/delete")
    public String deleteAdminEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminEventClient.deleteAdminEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:/admin/events";
    }

    @GetMapping({"/admin/applications/events", "/admin/event-participations", "/admin/event-applications"})
    public String adminEventParticipations(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String registrationStatus,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage,
            Model model
    ) {
        RegistrationStatus selectedRegistrationStatus = EventViewSupport.parseRegistrationStatus(registrationStatus);

        model.addAttribute("clubOptions", loadClubs());
        model.addAttribute("participations", Collections.emptyList());
        model.addAttribute("participationPage", null);
        model.addAttribute("selectedClubId", clubId);
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedRegistrationStatus", selectedRegistrationStatus);
        model.addAttribute("selectedEventStatus", eventStatus);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("registrationStatusValues", RegistrationStatus.values());
        model.addAttribute("eventStatusValues", EventStatus.values());
        model.addAttribute("successMessage", EventViewSupport.trimToNull(successMessage));
        model.addAttribute("errorMessage", EventViewSupport.trimToNull(errorMessage));

        try {
            PageResponse<EventParticipationDto> result = adminEventClient.getAdminParticipations(
                    clubId,
                    eventId,
                    selectedRegistrationStatus,
                    eventStatus,
                    EventViewSupport.trimToNull(q),
                    null,
                    page,
                    EventViewSupport.PARTICIPANTS_PAGE_SIZE,
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

        return "admin/event-participations";
    }

    @PostMapping("/admin/events/{eventId}/participants/{studentId}")
    public String updateAdminParticipation(
            @PathVariable Long eventId,
            @PathVariable Long studentId,
            @RequestParam RegistrationStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminEventClient.updateAdminParticipationStatus(
                    eventId,
                    studentId,
                    new UpdateEventParticipationStatusRequest(status)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Participation status updated.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toParticipantUpdateErrorMessage(ex));
        }

        return "redirect:/admin/event-participations?eventId=" + eventId;
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
            EventFormRequest form,
            List<ClubListDto> clubOptions,
            String workspaceLabel,
            String pageTitle,
            String pageSubtitle,
            String submitLabel,
            String formAction,
            String cancelHref,
            String eventMainImageUrl
    ) {
        model.addAttribute("workspaceLabel", workspaceLabel);
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
            return "Please choose a club.";
        }

        if (EventViewSupport.trimToNull(form.getTitle()) == null) {
            return "Event title is required.";
        }

        if (EventViewSupport.trimToNull(form.getDescription()) == null) {
            return "Description is required.";
        }

        if (EventViewSupport.parseDateTimeInput(form.getStartAt()) == null) {
            return "Start date and time are required.";
        }

        if (EventViewSupport.trimToNull(form.getEndAt()) != null && EventViewSupport.parseDateTimeInput(form.getEndAt()) == null) {
            return "End date and time must be valid.";
        }

        OffsetDateTime startAt = EventViewSupport.parseDateTimeInput(form.getStartAt());
        OffsetDateTime endAt = EventViewSupport.parseDateTimeInput(form.getEndAt());
        OffsetDateTime registrationDeadline = EventViewSupport.parseDateTimeInput(form.getRegistrationDeadline());

        if (endAt != null && startAt != null && endAt.isBefore(startAt)) {
            return "End date and time must be after the start.";
        }

        if (registrationDeadline != null && startAt != null && registrationDeadline.isAfter(startAt)) {
            return "Registration deadline must be on or before the event start.";
        }

        if (form.getCapacity() != null && form.getCapacity() < 1) {
            return "Capacity must be at least 1.";
        }

        if (form.getStatus() == null) {
            return "Please choose an event status.";
        }

        if (form.getAudience() == null) {
            return "Please choose who can see the event.";
        }

        return null;
    }

    private String validateMainImage(MultipartFile mainImage) {
        if (!hasFile(mainImage)) {
            return null;
        }

        if (mainImage.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            return "Main image must be 5 MB or smaller. Please choose another file.";
        }

        if (!isImageFile(mainImage)) {
            return "Main image must be an image file.";
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
            EventDto event = adminEventClient.getAdminEventById(id);
            return nonNull(event.mainImageUrl());
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String toEventsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the filters and try again.";
            default -> "Unable to load events right now. Please try again.";
        };
    }

    private String toParticipationLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the participation filters and try again.";
            default -> "Unable to load event participations right now. Please try again.";
        };
    }

    private String toEventSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> creating
                    ? "Please review the new event details and try again."
                    : "Please review the updated event details and try again.";
            case NOT_FOUND -> "The selected club or event is no longer available.";
            default -> creating
                    ? "Unable to create the event right now. Please try again."
                    : "Unable to save the event right now. Please try again.";
        };
    }

    private String toDeleteErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case NOT_FOUND -> "This event no longer exists.";
            default -> "Unable to delete the event right now. Please try again.";
        };
    }

    private String toParticipantUpdateErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case NOT_FOUND -> "This participation no longer exists.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Unable to update the participation status. Please refresh and try again.";
            default -> "Unable to update the participation right now. Please try again.";
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
