package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.clients.EventClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventListDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.MyEventDto;
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
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventController {
    private final EventClient eventClient;
    private final ClubClient clubClient;

    @GetMapping("/events")
    public String eventsPage(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String view,
            Model model
    ) {
        model.addAttribute("clubOptions", loadClubOptions(true));
        model.addAttribute("events", Collections.emptyList());
        model.addAttribute("eventPage", null);
        model.addAttribute("selectedClubId", clubId);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate.trim());
        model.addAttribute("toDate", toDate == null ? "" : toDate.trim());
        model.addAttribute("view", normalizeView(view));

        try {
            PageResponse<EventListDto> result = eventClient.getPublicEvents(
                    clubId,
                    EventViewSupport.trimToNull(q),
                    EventViewSupport.parseFromDate(fromDate),
                    EventViewSupport.parseToDate(toDate),
                    null,
                    page,
                    EventViewSupport.BROWSER_PAGE_SIZE,
                    EventViewSupport.EVENT_SORT
            );
            model.addAttribute("eventPage", result);
            model.addAttribute("events", result.getContent() == null ? Collections.emptyList() : result.getContent());
        } catch (FeignException ex) {
            model.addAttribute("loadErrorMessage", toEventsLoadErrorMessage(ex));
        }

        return "events/index";
    }

    @GetMapping("/events/{id}")
    public String eventDetails(
            @PathVariable Long id,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            Model model,
            HttpServletResponse response
    ) {
        try {
            EventDto event = eventClient.getPublicEventById(id);
            model.addAttribute("event", event);

            MyEventDto myEvent = null;
            if (EventViewSupport.isStudent(sessionUser)) {
                try {
                    myEvent = resolveMyEvent(id);
                } catch (RuntimeException ignored) {
                    // Event detail page should still render if registration lookup fails.
                }
            }

            RegistrationStatus myRegistrationStatus = myEvent != null ? myEvent.registrationStatus() : null;
            boolean canRegister = EventViewSupport.isStudent(sessionUser)
                    && myRegistrationStatus != RegistrationStatus.REGISTERED
                    && event.status() == EventStatus.PUBLISHED
                    && Boolean.TRUE.equals(event.registrationOpen());
            boolean canCancel = myRegistrationStatus == RegistrationStatus.REGISTERED && isCancellationStillAllowed(event);

            model.addAttribute("myEvent", myEvent);
            model.addAttribute("myRegistrationStatus", myRegistrationStatus);
            model.addAttribute("canRegister", canRegister);
            model.addAttribute("canCancelRegistration", canCancel);
            model.addAttribute("registrationWindowClosed", myRegistrationStatus == RegistrationStatus.REGISTERED && !canCancel);

            return "events/details";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.NOT_FOUND.value() || ex.status() == HttpStatus.FORBIDDEN.value()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                model.addAttribute("missingResourceType", "event");
                model.addAttribute("missingResourceId", id);
                return "errors/404";
            }

            throw ex;
        }
    }

    @PostMapping("/events/{id}/register")
    public String registerForEvent(
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            RedirectAttributes redirectAttributes
    ) {
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("success", "Please sign in to register for events.");
            return "redirect:/login";
        }

        if (!EventViewSupport.isStudent(sessionUser)) {
            redirectAttributes.addFlashAttribute("eventActionWarningMessage", "Only students can register for events.");
            return "redirect:" + resolveReturnTo(returnTo, "/events/" + id);
        }

        try {
            eventClient.register(id);
            redirectAttributes.addFlashAttribute("eventActionSuccessMessage", "Registration confirmed.");
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                redirectAttributes.addFlashAttribute("success", "Please sign in to register for events.");
                return "redirect:/login";
            }

            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute(
                        "eventActionWarningMessage",
                        EventViewSupport.firstNonBlank(
                                EventViewSupport.extractUserMessage(ex),
                                "You are not allowed to register for this event."
                        )
                );
            } else {
                redirectAttributes.addFlashAttribute("eventActionErrorMessage", toRegistrationErrorMessage(ex));
            }
        }

        return "redirect:" + resolveReturnTo(returnTo, "/events/" + id);
    }

    @PostMapping("/events/{id}/cancel")
    public String cancelEventRegistration(
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            RedirectAttributes redirectAttributes
    ) {
        if (sessionUser == null) {
            redirectAttributes.addFlashAttribute("success", "Please sign in to manage your event registrations.");
            return "redirect:/login";
        }

        if (!EventViewSupport.isStudent(sessionUser)) {
            redirectAttributes.addFlashAttribute("eventActionWarningMessage", "Only students can cancel event registrations.");
            return "redirect:" + resolveReturnTo(returnTo, "/events/" + id);
        }

        try {
            eventClient.cancelRegistration(id);
            redirectAttributes.addFlashAttribute("eventActionSuccessMessage", "Registration cancelled.");
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                redirectAttributes.addFlashAttribute("success", "Please sign in to manage your event registrations.");
                return "redirect:/login";
            }

            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                redirectAttributes.addFlashAttribute(
                        "eventActionWarningMessage",
                        EventViewSupport.firstNonBlank(
                                EventViewSupport.extractUserMessage(ex),
                                "You are not allowed to cancel this registration."
                        )
                );
            } else {
                redirectAttributes.addFlashAttribute("eventActionErrorMessage", toCancellationErrorMessage(ex));
            }
        }

        return "redirect:" + resolveReturnTo(returnTo, "/events/" + id);
    }

    @GetMapping("/me/events")
    public String myEvents(
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @ModelAttribute("sessionUser") AuthUserDto sessionUser,
            Model model
    ) {
        model.addAttribute("selectedStatus", status);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("events", Collections.emptyList());
        model.addAttribute("eventPage", null);

        if (sessionUser == null) {
            return "redirect:/login";
        }

        if (!EventViewSupport.isStudent(sessionUser)) {
            model.addAttribute("accessMessage", "Only students can view their event registrations.");
            return "me/events";
        }

        try {
            PageResponse<MyEventDto> result = eventClient.getMyEvents(
                    status,
                    null,
                    EventViewSupport.trimToNull(q),
                    null,
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

            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("accessMessage", "Only students can view their event registrations.");
                return "me/events";
            }

            model.addAttribute("loadErrorMessage", toMyEventsLoadErrorMessage(ex));
        }

        return "me/events";
    }

    private List<ClubListDto> loadClubOptions(boolean activeOnly) {
        try {
            PageResponse<ClubListDto> response = clubClient.getAll(activeOnly ? true : null, 0, 200, "name,asc");
            return response.getContent() == null ? List.of() : response.getContent();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private MyEventDto resolveMyEvent(Long eventId) {
        PageResponse<MyEventDto> response = eventClient.getMyEvents(
                null,
                null,
                null,
                null,
                0,
                200,
                EventViewSupport.PARTICIPATION_SORT
        );

        List<MyEventDto> events = response.getContent() == null ? List.of() : response.getContent();
        return events.stream()
                .filter(event -> event.eventId() != null && event.eventId().equals(eventId))
                .max(Comparator
                        .comparing(MyEventDto::registeredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MyEventDto::cancelledAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean isCancellationStillAllowed(EventDto event) {
        OffsetDateTime deadline = event.effectiveRegistrationDeadline();
        if (deadline == null) {
            return true;
        }

        return !deadline.isBefore(OffsetDateTime.now());
    }

    private String normalizeView(String view) {
        return "calendar".equalsIgnoreCase(view) ? "calendar" : "list";
    }

    private String resolveReturnTo(String returnTo, String fallback) {
        String normalized = EventViewSupport.trimToNull(returnTo);
        if (normalized == null || !normalized.startsWith("/")) {
            return fallback;
        }

        return normalized;
    }

    private String toEventsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the event filters and try again.";
            case NOT_FOUND -> "The events endpoint is not available right now.";
            default -> "Unable to load events right now. Please try again.";
        };
    }

    private String toMyEventsLoadErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Please review the selected status filter and try again.";
            case NOT_FOUND -> "Your event registrations are not available right now.";
            default -> "Unable to load your events right now. Please try again.";
        };
    }

    private String toRegistrationErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case CONFLICT -> "You already have a registration for this event.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "This event is no longer open for registration.";
            case NOT_FOUND -> "This event is not available.";
            default -> "Unable to complete registration right now. Please try again.";
        };
    }

    private String toCancellationErrorMessage(FeignException ex) {
        String extracted = EventViewSupport.extractUserMessage(ex);
        if (!extracted.isBlank()) {
            return extracted;
        }

        return switch (EventViewSupport.resolveStatus(ex)) {
            case CONFLICT -> "This registration has already been updated.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "This registration can no longer be cancelled.";
            case NOT_FOUND -> "This event registration was not found.";
            default -> "Unable to cancel the registration right now. Please try again.";
        };
    }
}
