package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.AdminTeacherClient;
import com.school.ppmg.student_clubs_system_client.clients.AnnouncementClient;
import com.school.ppmg.student_clubs_system_client.clients.ClubClient;
import com.school.ppmg.student_clubs_system_client.clients.EventClient;
import com.school.ppmg.student_clubs_system_client.clients.MembershipApplicationClient;
import com.school.ppmg.student_clubs_system_client.controllers.support.EventViewSupport;
import com.school.ppmg.student_clubs_system_client.dtos.announcement.AnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.auth.UserDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.AddClubTeachersRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.CreateClubRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.CreateMembershipApplicationRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.MediaDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.MembershipApplicationDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.TeacherDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpsertClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import com.school.ppmg.student_clubs_system_client.dtos.event.EventListDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class ClubController {
    private static final long MAX_IMAGE_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private final AdminTeacherClient adminTeacherClient;
    private final AnnouncementClient announcementClient;
    private final ClubClient clubClient;
    private final EventClient eventClient;
    private final MembershipApplicationClient membershipApplicationClient;
    private static final int PUBLIC_PAGE_SIZE = 9;
    private static final int ADMIN_PAGE_SIZE = 10;

    @GetMapping("/clubs")
    public String clubsPage(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        String normalizedQuery = trimToNull(q);
        PageResponse<ClubListDto> result = clubClient.getAll(true, normalizedQuery, page, PUBLIC_PAGE_SIZE, null);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());
        model.addAttribute("q", normalizedQuery == null ? "" : normalizedQuery);

        return "clubs/index";
    }

    @GetMapping("/admin/clubs")
    public String adminClubsPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String success,
            @ModelAttribute("successMessage") String flashSuccessMessage,
            @ModelAttribute("errorMessage") String flashErrorMessage,
            Model model
    ) {
        PageResponse<ClubListDto> result = clubClient.getAll(active, null, page, ADMIN_PAGE_SIZE, null);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());
        model.addAttribute("active", active);
        model.addAttribute("successMessage", firstNonBlank(flashSuccessMessage, successMessage(success)));
        model.addAttribute("errorMessage", trimToNull(flashErrorMessage));

        return "admin/clubs";
    }

    @GetMapping("/admin/clubs/create")
    public String createClubPage(Model model) {
        populateCreateFormModel(model, "", "", "", "", "", "", true, List.of());
        return "admin/club-form";
    }

    @PostMapping("/admin/clubs/create")
    public String createClub(
            @ModelAttribute CreateClubRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String normalizedName = normalizeRequiredText(request.getName());
        String normalizedDescription = normalizeRequiredText(request.getDescription());
        String normalizedScheduleText = normalizeOptionalText(request.getScheduleText());
        String normalizedRoom = normalizeOptionalText(request.getRoom());
        String normalizedContactEmail = normalizeOptionalText(request.getContactEmail());
        String normalizedContactPhone = normalizeOptionalText(request.getContactPhone());
        List<Long> teacherIds = normalizeTeacherIds(request.getTeacherIds());
        boolean isActive = Boolean.TRUE.equals(request.getIsActive());
        MultipartFile mainImage = hasFile(request.getMainImage()) ? request.getMainImage() : null;
        List<MultipartFile> mediaFiles = normalizeFiles(request.getMediaFiles());

        if (normalizedName.isBlank()) {
            populateCreateFormModel(
                    model,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    teacherIds
            );
            model.addAttribute("errorMessage", "Club Name is required.");
            return "admin/club-form";
        }

        if (normalizedDescription.isBlank()) {
            populateCreateFormModel(
                    model,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    teacherIds
            );
            model.addAttribute("errorMessage", "Description is required.");
            return "admin/club-form";
        }

        if (mainImage != null && mainImage.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            populateCreateFormModel(
                    model,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    teacherIds
            );
            model.addAttribute("errorMessage", "Main image must be 5 MB or smaller. Please choose another file.");
            return "admin/club-form";
        }

        if (mainImage != null && !isImageFile(mainImage)) {
            populateCreateFormModel(
                    model,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    teacherIds
            );
            model.addAttribute("errorMessage", "Main image must be an image file.");
            return "admin/club-form";
        }

        for (MultipartFile mediaFile : mediaFiles) {
            if (mediaFile.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
                populateCreateFormModel(
                        model,
                        normalizedName,
                        normalizedDescription,
                        normalizedScheduleText,
                        normalizedRoom,
                        normalizedContactEmail,
                        normalizedContactPhone,
                        isActive,
                        teacherIds
                );
                model.addAttribute(
                        "errorMessage",
                        "Each uploaded media file must be 5 MB or smaller. Please choose another file."
                );
                return "admin/club-form";
            }

            if (!isImageFile(mediaFile)) {
                populateCreateFormModel(
                        model,
                        normalizedName,
                        normalizedDescription,
                        normalizedScheduleText,
                        normalizedRoom,
                        normalizedContactEmail,
                        normalizedContactPhone,
                        isActive,
                        teacherIds
                );
                model.addAttribute("errorMessage", "Club media uploads must be image files.");
                return "admin/club-form";
            }
        }

        request.setName(normalizedName);
        request.setDescription(normalizedDescription);
        request.setScheduleText(normalizedScheduleText);
        request.setRoom(normalizedRoom);
        request.setContactEmail(normalizedContactEmail);
        request.setContactPhone(normalizedContactPhone);
        request.setIsActive(isActive);
        request.setTeacherIds(teacherIds.isEmpty() ? null : teacherIds);
        request.setMainImage(mainImage);
        request.setMediaFiles(mediaFiles);

        try {
            if (mediaFiles.isEmpty()) {
                ClubDto createdClub = clubClient.create(request.toCreateClubDto());
                if (mainImage != null) {
                    try {
                        clubClient.uploadMainImage(createdClub.id(), mainImage);
                    } catch (FeignException ex) {
                        redirectAttributes.addFlashAttribute(
                                "errorMessage",
                                firstNonBlank(
                                        extractUserMessage(ex),
                                        "Club created, but the main image upload failed. You can try again from the edit page."
                                )
                        );
                        return "redirect:/admin/clubs/" + createdClub.id() + "/edit";
                    }
                }
            } else {
                clubClient.createMultipart(request);
            }
            return "redirect:/admin/clubs?success=created";
        } catch (FeignException ex) {
            populateCreateFormModel(
                    model,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    teacherIds
            );
            model.addAttribute("errorMessage", toCreateClubSaveErrorMessage(ex, request));
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
            populateEditFormModel(model, club, extractSelectedTeacherIds(model));
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
        String normalizedDescription = normalizeRequiredText(description);
        String normalizedScheduleText = normalizeOptionalText(scheduleText);
        String normalizedRoom = normalizeOptionalText(room);
        String normalizedContactEmail = normalizeOptionalText(contactEmail);
        String normalizedContactPhone = normalizeOptionalText(contactPhone);

        if (normalizedName.isBlank()) {
            populateEditFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveAssignedTeachers(id),
                    extractSelectedTeacherIds(model)
            );
            model.addAttribute("errorMessage", "Club Name is required.");
            return "admin/club-form";
        }

        if (normalizedDescription.isBlank()) {
            populateEditFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveAssignedTeachers(id),
                    extractSelectedTeacherIds(model)
            );
            model.addAttribute("errorMessage", "Description is required.");
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
            populateEditFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveAssignedTeachers(id),
                    extractSelectedTeacherIds(model)
            );
            model.addAttribute("errorMessage", toClubSaveErrorMessage(ex));
            return "admin/club-form";
        }
    }

    @PostMapping("/admin/clubs/{id}/teachers")
    public String addClubTeachers(
            @PathVariable Long id,
            @RequestParam(name = "teacherIds", required = false) List<Long> teacherIds,
            RedirectAttributes redirectAttributes
    ) {
        List<Long> normalizedTeacherIds = normalizeTeacherIds(teacherIds);
        if (normalizedTeacherIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Select at least one teacher to add.");
            redirectAttributes.addFlashAttribute("selectedTeacherIds", normalizedTeacherIds);
            return redirectToAdminClubEdit(id);
        }

        try {
            clubClient.addTeachers(id, new AddClubTeachersRequest(normalizedTeacherIds));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    normalizedTeacherIds.size() == 1 ? "Teacher added successfully." : "Teachers added successfully."
            );
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toTeacherAssignmentErrorMessage(ex, true));
            redirectAttributes.addFlashAttribute("selectedTeacherIds", normalizedTeacherIds);
        }

        return redirectToAdminClubEdit(id);
    }

    @PostMapping("/admin/clubs/{id}/teachers/{teacherId}/remove")
    public String removeClubTeacher(
            @PathVariable Long id,
            @PathVariable Long teacherId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clubClient.removeTeacher(id, teacherId);
            redirectAttributes.addFlashAttribute("successMessage", "Teacher removed successfully.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toTeacherAssignmentErrorMessage(ex, false));
        }

        return redirectToAdminClubEdit(id);
    }

    @PostMapping("/admin/clubs/{id}/main-image")
    public String uploadMainImage(
            @PathVariable Long id,
            @RequestParam("mainImage") MultipartFile mainImage,
            RedirectAttributes redirectAttributes
    ) {
        if (!hasFile(mainImage)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please choose an image to upload.");
            return redirectToAdminClubEdit(id);
        }

        if (mainImage.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Main image must be 5 MB or smaller. Please choose another file."
            );
            return redirectToAdminClubEdit(id);
        }

        if (!isImageFile(mainImage)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Main image must be an image file.");
            return redirectToAdminClubEdit(id);
        }

        try {
            clubClient.uploadMainImage(id, mainImage);
            redirectAttributes.addFlashAttribute("successMessage", "Main image updated successfully.");
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    firstNonBlank(extractUserMessage(ex), "Unable to upload the main image right now. Please try again.")
            );
        }

        return redirectToAdminClubEdit(id);
    }

    @PostMapping("/admin/clubs/{id}/delete")
    public String deleteClub(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clubClient.delete(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Club deleted. Future events were cancelled and active memberships were marked as left."
            );
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", toClubDeleteErrorMessage(ex));
        }

        return "redirect:/admin/clubs";
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

            MembershipRequestStatus myApplicationStatus = null;
            if (isStudent(sessionUser)) {
                try {
                    myApplicationStatus = resolveMyApplicationStatus(id);
                } catch (RuntimeException ignored) {
                    // Club page should still render even if membership status lookup fails.
                }
            }

            model.addAttribute("myApplicationStatus", myApplicationStatus);
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
            PageResponse<EventListDto> result = eventClient.getPublicEvents(
                    id,
                    null,
                    null,
                    null,
                    null,
                    0,
                    EventViewSupport.TAB_PAGE_SIZE,
                    null
            );
            model.addAttribute("events", result.getContent() == null ? List.of() : result.getContent());
            model.addAttribute("clubId", id);
        } catch (Exception ex) {
            model.addAttribute("error", "Events are not available yet.");
        }

        return "clubs/tabs/events :: content";
    }

    @GetMapping("/clubs/{id}/tabs/media")
    public String clubMediaTab(@PathVariable Long id, Model model) {
        try {
            ClubDto club = clubClient.getById(id);
            List<MediaDto> media = club.media() == null ? List.of() : club.media();
            model.addAttribute("media", media);
        } catch (Exception ex) {
            model.addAttribute("error", "Media is not available yet.");
        }

        return "clubs/tabs/media :: content";
    }

    @GetMapping("/clubs/{id}/tabs/announcements")
    public String clubAnnouncementsTab(@PathVariable Long id, Model model) {
        try {
            PageResponse<AnnouncementDto> result = announcementClient.getPublicAnnouncements(
                    id,
                    null,
                    null,
                    null,
                    0,
                    EventViewSupport.TAB_PAGE_SIZE,
                    null
            );
            List<AnnouncementDto> announcements = result.getContent() == null ? List.of() : result.getContent();
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

    private void populateCreateFormModel(
            Model model,
            String name,
            String description,
            String scheduleText,
            String room,
            String contactEmail,
            String contactPhone,
            boolean isActive,
            List<Long> teacherIds
    ) {
        populateFormModel(
                model,
                "create",
                null,
                name,
                description,
                scheduleText,
                room,
                contactEmail,
                contactPhone,
                isActive
        );

        List<TeacherDto> teacherOptions = loadTeacherOptions();

        model.addAttribute("teacherOptions", teacherOptions);
        model.addAttribute("selectedTeacherIds", normalizeTeacherIds(teacherIds));
        model.addAttribute("teacherOptionsAvailable", !teacherOptions.isEmpty());
    }

    private void populateEditFormModel(Model model, ClubDto club, List<Long> selectedTeacherIds) {
        populateEditFormModel(
                model,
                club.id(),
                nonNull(club.name()),
                nonNull(club.description()),
                nonNull(club.scheduleText()),
                nonNull(club.room()),
                nonNull(club.contactEmail()),
                nonNull(club.contactPhone()),
                club.isActive() == null || club.isActive(),
                normalizeTeachers(club.teachers()),
                selectedTeacherIds
        );
    }

    private void populateEditFormModel(
            Model model,
            Long clubId,
            String name,
            String description,
            String scheduleText,
            String room,
            String contactEmail,
            String contactPhone,
            boolean isActive,
            List<TeacherDto> assignedTeachers,
            List<Long> selectedTeacherIds
    ) {
        populateFormModel(
                model,
                "edit",
                clubId,
                name,
                description,
                scheduleText,
                room,
                contactEmail,
                contactPhone,
                isActive
        );

        List<TeacherDto> normalizedAssignedTeachers = normalizeTeachers(assignedTeachers);
        List<TeacherDto> allTeacherOptions = loadTeacherOptions();
        List<TeacherDto> availableTeacherOptions = new ArrayList<>();

        for (TeacherDto teacher : allTeacherOptions) {
            if (!containsTeacher(normalizedAssignedTeachers, teacher.id())) {
                availableTeacherOptions.add(teacher);
            }
        }

        model.addAttribute("assignedTeachers", normalizedAssignedTeachers);
        model.addAttribute("availableTeacherOptions", availableTeacherOptions);
        model.addAttribute("selectedTeacherIds", normalizeTeacherIds(selectedTeacherIds));
        model.addAttribute("teacherOptionsAvailable", !allTeacherOptions.isEmpty());
        model.addAttribute("availableTeacherOptionsAvailable", !availableTeacherOptions.isEmpty());
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

        if ("deleted".equalsIgnoreCase(success)) {
            return "Club deleted successfully.";
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

    private MembershipRequestStatus resolveMyApplicationStatus(Long clubId) {
        PageResponse<MembershipApplicationDto> response = membershipApplicationClient.getMyApplications(
                null,
                clubId,
                null,
                0,
                1,
                null
        );

        List<MembershipApplicationDto> applications = response.getContent() == null ? List.of() : response.getContent();
        return applications
                .stream()
                .filter(application -> application.clubId() != null && application.clubId().equals(clubId))
                .max(Comparator
                        .comparing(
                                MembershipApplicationDto::createdAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                MembershipApplicationDto::id,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .map(MembershipApplicationDto::status)
                .orElse(null);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
            case NOT_FOUND -> "Requested resource not found.";
            default -> "Unable to save the club right now. Please try again.";
        };
    }

    private String toCreateClubSaveErrorMessage(FeignException ex, CreateClubRequest request) {
        String message = toClubSaveErrorMessage(ex);
        if (!hasFile(request.getMainImage()) && normalizeFiles(request.getMediaFiles()).isEmpty()) {
            return message;
        }

        return message + " Please re-select any files before trying again.";
    }

    private String toTeacherAssignmentErrorMessage(FeignException ex, boolean addOperation) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String fallback = switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> addOperation
                    ? "Please review the selected teachers and try again."
                    : "Unable to remove this teacher assignment right now.";
            case UNAUTHORIZED, FORBIDDEN -> "You are not authorized to manage teachers for this club.";
            case NOT_FOUND -> addOperation
                    ? "The club or selected teacher could not be found."
                    : "The teacher assignment could not be found.";
            default -> addOperation
                    ? "Unable to add teachers right now. Please try again."
                    : "Unable to remove the teacher right now. Please try again.";
        };

        return firstNonBlank(extractUserMessage(ex), fallback);
    }

    private String toClubDeleteErrorMessage(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String fallback = switch (status) {
            case FORBIDDEN, UNAUTHORIZED -> "You are not authorized to delete this club.";
            case NOT_FOUND -> "This club no longer exists.";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "This club could not be deleted right now. Please refresh and try again.";
            default -> "Unable to delete the club right now. Please try again.";
        };

        return firstNonBlank(extractUserMessage(ex), fallback);
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

    private String redirectToAdminClubEdit(Long id) {
        return "redirect:/admin/clubs/" + id + "/edit";
    }

    private List<TeacherDto> loadTeacherOptions() {
        try {
            PageResponse<UserDto> response = adminTeacherClient.getAllTeachers(null, 0, 200);
            List<UserDto> teachers = response.getContent() == null ? List.of() : response.getContent();

            return teachers.stream()
                    .filter(teacher -> teacher != null && teacher.id() != null)
                    .map(teacher -> new TeacherDto(
                            teacher.id(),
                            buildTeacherOptionLabel(teacher)
                    ))
                    .sorted(Comparator.comparing(
                            TeacherDto::fullName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    ))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String buildTeacherOptionLabel(UserDto teacher) {
        String firstName = nonNull(teacher.firstName()).trim();
        String lastName = nonNull(teacher.lastName()).trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }

        String email = nonNull(teacher.email()).trim();
        if (!email.isEmpty()) {
            return email;
        }

        return "Teacher #" + teacher.id();
    }

    private List<TeacherDto> normalizeTeachers(List<TeacherDto> teachers) {
        if (teachers == null || teachers.isEmpty()) {
            return List.of();
        }

        List<TeacherDto> normalized = new ArrayList<>();
        for (TeacherDto teacher : teachers) {
            if (teacher != null && teacher.id() != null) {
                normalized.add(teacher);
            }
        }
        return normalized;
    }

    private boolean containsTeacher(List<TeacherDto> teachers, Long teacherId) {
        if (teacherId == null) {
            return false;
        }

        for (TeacherDto teacher : teachers) {
            if (teacher != null && teacherId.equals(teacher.id())) {
                return true;
            }
        }
        return false;
    }

    private List<TeacherDto> resolveAssignedTeachers(Long clubId) {
        try {
            return normalizeTeachers(clubClient.getById(clubId).teachers());
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private List<Long> normalizeTeacherIds(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }

        List<Long> normalized = new ArrayList<>();
        for (Long teacherId : teacherIds) {
            if (teacherId != null && teacherId > 0 && !normalized.contains(teacherId)) {
                normalized.add(teacherId);
            }
        }
        return normalized;
    }

    private List<Long> extractSelectedTeacherIds(Model model) {
        Object rawValue = model.asMap().get("selectedTeacherIds");
        if (!(rawValue instanceof List<?> rawTeacherIds)) {
            return List.of();
        }

        List<Long> selectedTeacherIds = new ArrayList<>();
        for (Object rawTeacherId : rawTeacherIds) {
            if (rawTeacherId instanceof Long teacherId) {
                selectedTeacherIds.add(teacherId);
            } else if (rawTeacherId instanceof Number teacherId) {
                selectedTeacherIds.add(teacherId.longValue());
            } else if (rawTeacherId instanceof String teacherIdText) {
                try {
                    selectedTeacherIds.add(Long.parseLong(teacherIdText));
                } catch (NumberFormatException ignored) {
                    // Ignore invalid flash values.
                }
            }
        }

        return normalizeTeacherIds(selectedTeacherIds);
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<MultipartFile> normalized = new ArrayList<>();
        for (MultipartFile file : files) {
            if (hasFile(file)) {
                normalized.add(file);
            }
        }
        return normalized;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private boolean isImageFile(MultipartFile file) {
        if (!hasFile(file)) {
            return false;
        }

        return isImageContentType(file.getContentType());
    }

    private boolean isImageContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        return contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }
}
