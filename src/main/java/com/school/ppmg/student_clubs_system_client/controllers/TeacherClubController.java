package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.TeacherClubClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ManageClubDto;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TeacherClubController {
    private static final int PAGE_SIZE = 9;

    private final TeacherClubClient teacherClubClient;

    @GetMapping("/teacher")
    public String teacherHome() {
        return "redirect:/teacher/clubs";
    }

    @GetMapping("/teacher/clubs")
    public String teacherClubsPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error,
            Model model
    ) {
        String resolvedSort = (sort == null || sort.isBlank()) ? "name,asc" : sort;
        PageResponse<ClubListDto> result = teacherClubClient.getManagedClubs(active, page, PAGE_SIZE, resolvedSort);

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());
        model.addAttribute("active", active);
        model.addAttribute("sort", resolvedSort);
        model.addAttribute("size", PAGE_SIZE);
        model.addAttribute("successMessage", successMessage(success));
        model.addAttribute("errorMessage", listErrorMessage(error));

        return "teacher/clubs";
    }

    @GetMapping("/teacher/clubs/{id}/edit")
    public String editManagedClubPage(
            @PathVariable Long id,
            @RequestParam(required = false) String success,
            @ModelAttribute("successMessage") String uploadSuccessMessage,
            @ModelAttribute("errorMessage") String uploadErrorMessage,
            Model model,
            HttpServletResponse response
    ) {
        try {
            ClubDto club = teacherClubClient.getManagedClubById(id);
            populateFormModel(
                    model,
                    id,
                    nonNull(club.name()),
                    nonNull(club.description()),
                    nonNull(club.scheduleText()),
                    nonNull(club.room()),
                    nonNull(club.contactEmail()),
                    nonNull(club.contactPhone()),
                    club.isActive() == null || club.isActive(),
                    nonNull(club.mainImageUrl())
            );
            model.addAttribute("successMessage", firstNonBlank(uploadSuccessMessage, successMessage(success)));
            model.addAttribute("errorMessage", uploadErrorMessage);
            return "teacher/club-form";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/teacher/clubs?error=forbidden";
            }
            throw ex;
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

    @PostMapping("/teacher/clubs/{id}/edit")
    public String updateManagedClub(
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
            populateFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("errorMessage", "Club Name is required.");
            return "teacher/club-form";
        }

        if (normalizedDescription.isBlank()) {
            populateFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("errorMessage", "Description is required.");
            return "teacher/club-form";
        }

        try {
            teacherClubClient.updateManagedClub(id, new ManageClubDto(
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive
            ));
            return "redirect:/teacher/clubs?success=updated";
        } catch (FeignException.NotFound ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("missingClubId", id);
            return "errors/404";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/teacher/clubs?error=forbidden";
            }

            populateFormModel(
                    model,
                    id,
                    normalizedName,
                    normalizedDescription,
                    normalizedScheduleText,
                    normalizedRoom,
                    normalizedContactEmail,
                    normalizedContactPhone,
                    isActive,
                    resolveCurrentMainImageUrl(id)
            );
            model.addAttribute("errorMessage", toClubSaveErrorMessage(ex));
            return "teacher/club-form";
        }
    }

    @PostMapping("/teacher/clubs/{id}/main-image")
    public String uploadManagedClubMainImage(
            @PathVariable Long id,
            @RequestParam("mainImage") MultipartFile mainImage,
            RedirectAttributes redirectAttributes
    ) {
        if (!hasFile(mainImage)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please choose an image to upload.");
            return "redirect:/teacher/clubs/" + id + "/edit";
        }

        try {
            teacherClubClient.uploadManagedClubMainImage(id, mainImage);
            redirectAttributes.addFlashAttribute("successMessage", "Main image updated successfully.");
            return "redirect:/teacher/clubs/" + id + "/edit";
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/teacher/clubs?error=forbidden";
            }

            if (ex.status() == HttpStatus.NOT_FOUND.value()) {
                return "redirect:/teacher/clubs?error=not-found";
            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    firstNonBlank(extractUserMessage(ex), "Unable to upload the main image right now. Please try again.")
            );
            return "redirect:/teacher/clubs/" + id + "/edit";
        }
    }

    private void populateFormModel(
            Model model,
            Long clubId,
            String name,
            String description,
            String scheduleText,
            String room,
            String contactEmail,
            String contactPhone,
            boolean isActive,
            String mainImageUrl
    ) {
        model.addAttribute("clubId", clubId);
        model.addAttribute("clubName", nonNull(name));
        model.addAttribute("clubDescription", nonNull(description));
        model.addAttribute("clubScheduleText", nonNull(scheduleText));
        model.addAttribute("clubRoom", nonNull(room));
        model.addAttribute("clubContactEmail", nonNull(contactEmail));
        model.addAttribute("clubContactPhone", nonNull(contactPhone));
        model.addAttribute("clubIsActive", isActive);
        model.addAttribute("clubMainImageUrl", nonNull(mainImageUrl));
        model.addAttribute("pageTitle", "Edit Club");
        model.addAttribute("pageSubtitle", "Update details for the clubs you manage.");
        model.addAttribute("submitLabel", "Save Changes");
    }

    private String successMessage(String success) {
        if (success == null || success.isBlank()) {
            return null;
        }

        if ("updated".equalsIgnoreCase(success)) {
            return "Club updated successfully.";
        }

        return null;
    }

    private String listErrorMessage(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }

        if ("forbidden".equalsIgnoreCase(error)) {
            return "You can only manage clubs assigned to you.";
        }

        if ("not-found".equalsIgnoreCase(error)) {
            return "That club is no longer available.";
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

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private String resolveCurrentMainImageUrl(Long id) {
        try {
            ClubDto club = teacherClubClient.getManagedClubById(id);
            return nonNull(club.mainImageUrl());
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String toClubSaveErrorMessage(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String content = ex.contentUTF8();
        if (content != null && !content.isBlank()) {
            String extracted = extractUserMessage(ex);
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
            case UNAUTHORIZED, FORBIDDEN -> "You are not authorized to manage this club.";
            case NOT_FOUND -> "Requested resource not found.";
            default -> "Unable to save the club right now. Please try again.";
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
