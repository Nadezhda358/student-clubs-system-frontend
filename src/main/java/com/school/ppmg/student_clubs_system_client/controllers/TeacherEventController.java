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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TeacherEventController {
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
                    EventViewSupport.BROWSER_PAGE_SIZE,
                    EventViewSupport.EVENT_SORT
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
            redirectAttributes.addFlashAttribute("errorMessage", "You need at least one managed club before creating events.");
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
                "Teacher Workspace",
                "Create Event",
                "Plan a new event for one of the clubs you manage.",
                "Create Event",
                "/teacher/events/create",
                buildTeacherEventsHref(form.getClubId())
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
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Teacher Workspace",
                    "Create Event",
                    "Plan a new event for one of the clubs you manage.",
                    "Create Event",
                    "/teacher/events/create",
                    buildTeacherEventsHref(form.getClubId())
            );
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/event-form";
        }

        try {
            teacherEventClient.createTeacherEvent(toUpsertEventDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Event created successfully.");
            return "redirect:" + buildTeacherEventsHref(form.getClubId());
        } catch (FeignException ex) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Teacher Workspace",
                    "Create Event",
                    "Plan a new event for one of the clubs you manage.",
                    "Create Event",
                    "/teacher/events/create",
                    buildTeacherEventsHref(form.getClubId())
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
                    "Teacher Workspace",
                    "Edit Event",
                    "Adjust the schedule, registration settings, or capacity for this event.",
                    "Save Changes",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(event.clubId())
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
                redirectAttributes.addFlashAttribute("errorMessage", "You can only manage events for clubs assigned to you.");
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
        if (validationMessage != null) {
            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Teacher Workspace",
                    "Edit Event",
                    "Adjust the schedule, registration settings, or capacity for this event.",
                    "Save Changes",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(form.getClubId())
            );
            model.addAttribute("eventId", id);
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/event-form";
        }

        try {
            teacherEventClient.updateTeacherEvent(id, toUpsertEventDto(form));
            redirectAttributes.addFlashAttribute("successMessage", "Event updated successfully.");
            return "redirect:" + buildTeacherEventsHref(form.getClubId());
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingResourceType", "event");
            model.addAttribute("missingResourceId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only manage events for clubs assigned to you.");
                return "redirect:/teacher/events";
            }

            populateFormModel(
                    model,
                    form,
                    clubOptions,
                    "Teacher Workspace",
                    "Edit Event",
                    "Adjust the schedule, registration settings, or capacity for this event.",
                    "Save Changes",
                    "/teacher/events/" + id + "/edit",
                    buildTeacherEventsHref(form.getClubId())
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
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toDeleteErrorMessage(ex));
        }

        return "redirect:" + buildTeacherEventsHref(clubId);
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
                    EventViewSupport.PARTICIPANTS_PAGE_SIZE,
                    EventViewSupport.PARTICIPATION_SORT
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
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to view participants for this event.");
                return "redirect:/teacher/events";
            }

            throw ex;
        }
    }

    @PostMapping("/teacher/events/{eventId}/participants/{studentId}")
    public String updateTeacherParticipation(
            @PathVariable Long eventId,
            @PathVariable Long studentId,
            @RequestParam RegistrationStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            teacherEventClient.updateTeacherParticipationStatus(
                    eventId,
                    studentId,
                    new UpdateEventParticipationStatusRequest(status)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Participant status updated.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toParticipantUpdateErrorMessage(ex));
        }

        return "redirect:/teacher/events/" + eventId + "/participants";
    }

    private List<ClubListDto> loadManagedClubs() {
        try {
            PageResponse<ClubListDto> response = teacherClubClient.getManagedClubs(null, 0, 200, "name,asc");
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
            String cancelHref
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
            case FORBIDDEN -> "You can only manage events for clubs assigned to you.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the filters and try again.";
            default -> "Unable to load your events right now. Please try again.";
        };
    }

    private String toEventSaveErrorMessage(FeignException ex, boolean creating) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case FORBIDDEN -> "You can only manage events for clubs assigned to you.";
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
            case FORBIDDEN -> "You can only delete events for clubs assigned to you.";
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
            case FORBIDDEN -> "You are not authorized to update this participant.";
            case NOT_FOUND -> "This participant registration no longer exists.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Unable to update the participant status. Please refresh and try again.";
            default -> "Unable to update the participant right now. Please try again.";
        };
    }
}
