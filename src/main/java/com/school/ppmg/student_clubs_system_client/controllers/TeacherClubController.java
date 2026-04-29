package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.TeacherClubClient;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ManageClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.MediaDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class TeacherClubController {
    private static final long MAX_IMAGE_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int PAGE_SIZE = 10;

    private final TeacherClubClient teacherClubClient;
    private final Validator validator;

    @GetMapping("/teacher")
    public String teacherHome() {
        return "redirect:/teacher/clubs";
    }

    @GetMapping("/teacher/clubs")
    public String teacherClubsPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error,
            @ModelAttribute("successMessage") String flashSuccessMessage,
            @ModelAttribute("errorMessage") String flashErrorMessage,
            Model model
    ) {
        String normalizedQuery = normalizeOptionalText(q);
        PageResponse<ClubListDto> result = teacherClubClient.getManagedClubs(
                active,
                normalizedQuery,
                page,
                PAGE_SIZE,
                null
        );

        model.addAttribute("page", result);
        model.addAttribute("clubs", result.getContent());
        model.addAttribute("active", active);
        model.addAttribute("q", nonNull(normalizedQuery));
        model.addAttribute("successMessage", firstNonBlank(flashSuccessMessage, successMessage(success)));
        model.addAttribute("errorMessage", firstNonBlank(flashErrorMessage, listErrorMessage(error)));

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
                    nonNull(club.mainImageUrl()),
                    normalizeMedia(club.media())
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
            if ((message.contains("not found") || message.contains("не е намер"))) {
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
            model.addAttribute("errorMessage", "Името на клуба е задължително.");
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
            model.addAttribute("errorMessage", "Описанието е задължително.");
            return "teacher/club-form";
        }

        ManageClubDto dto = new ManageClubDto(
                normalizedName,
                normalizedDescription,
                normalizedScheduleText,
                normalizedRoom,
                normalizedContactEmail,
                normalizedContactPhone,
                isActive
        );
        String validationMessage = firstValidationMessage(dto);
        if (validationMessage != null) {
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
            model.addAttribute("errorMessage", validationMessage);
            return "teacher/club-form";
        }

        try {
            teacherClubClient.updateManagedClub(id, dto);
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
            redirectAttributes.addFlashAttribute("errorMessage", "Изберете изображение за качване.");
            return "redirect:/teacher/clubs/" + id + "/edit";
        }

        if (mainImage.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Основното изображение трябва да е 5 MB или по-малко. Изберете друг файл."
            );
            return "redirect:/teacher/clubs/" + id + "/edit";
        }

        if (!isImageFile(mainImage)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Основното изображение трябва да е файл с изображение.");
            return "redirect:/teacher/clubs/" + id + "/edit";
        }

        try {
            teacherClubClient.uploadManagedClubMainImage(id, mainImage);
            redirectAttributes.addFlashAttribute("successMessage", "Основното изображение е обновено успешно.");
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
                    firstNonBlank(extractUserMessage(ex), "Основното изображение не може да бъде качено в момента. Опитайте отново.")
            );
            return "redirect:/teacher/clubs/" + id + "/edit";
        }
    }

    @PostMapping("/teacher/clubs/{id}/media")
    public String uploadManagedClubMedia(
            @PathVariable Long id,
            @RequestParam(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            RedirectAttributes redirectAttributes
    ) {
        List<MultipartFile> files = normalizeFiles(mediaFiles);
        String validationMessage = validateMediaFiles(files);
        if (validationMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationMessage);
            return redirectToTeacherClubEdit(id);
        }

        try {
            teacherClubClient.uploadManagedClubMedia(id, files.toArray(MultipartFile[]::new));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    files.size() == 1 ? "Медийното изображение е добавено успешно." : "Медийните изображения са добавени успешно."
            );
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/teacher/clubs?error=forbidden";
            }

            if (ex.status() == HttpStatus.NOT_FOUND.value()) {
                return "redirect:/teacher/clubs?error=not-found";
            }

            redirectAttributes.addFlashAttribute("errorMessage", toClubMediaErrorMessage(ex, true));
        }

        return redirectToTeacherClubEdit(id);
    }

    @PostMapping("/teacher/clubs/{id}/media/{mediaId}/remove")
    public String removeManagedClubMedia(
            @PathVariable Long id,
            @PathVariable Long mediaId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            teacherClubClient.removeManagedClubMedia(id, mediaId);
            redirectAttributes.addFlashAttribute("successMessage", "Медийното изображение е премахнато успешно.");
        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.FORBIDDEN.value()) {
                return "redirect:/teacher/clubs?error=forbidden";
            }

            if (ex.status() == HttpStatus.NOT_FOUND.value()) {
                return "redirect:/teacher/clubs?error=not-found";
            }

            redirectAttributes.addFlashAttribute("errorMessage", toClubMediaErrorMessage(ex, false));
        }

        return redirectToTeacherClubEdit(id);
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
        populateFormModel(
                model,
                clubId,
                name,
                description,
                scheduleText,
                room,
                contactEmail,
                contactPhone,
                isActive,
                mainImageUrl,
                resolveCurrentMedia(clubId)
        );
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
            String mainImageUrl,
            List<MediaDto> media
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
        model.addAttribute("clubMedia", normalizeMedia(media));
        model.addAttribute("pageTitle", "Редактирай клуб");
        model.addAttribute("pageSubtitle", "Обновявайте данните за клубовете, които управлявате.");
        model.addAttribute("submitLabel", "Запази промените");
    }

    private String successMessage(String success) {
        if (success == null || success.isBlank()) {
            return null;
        }

        if ("updated".equalsIgnoreCase(success)) {
            return "Клубът е обновен успешно.";
        }

        if ("deleted".equalsIgnoreCase(success)) {
            return "Клубът е изтрит успешно.";
        }

        return null;
    }

    private String listErrorMessage(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }

        if ("forbidden".equalsIgnoreCase(error)) {
            return "Можете да управлявате само клубове, които са ви назначени.";
        }

        if ("not-found".equalsIgnoreCase(error)) {
            return "Този клуб вече не е наличен.";
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

    private String redirectToTeacherClubEdit(Long id) {
        return "redirect:/teacher/clubs/" + id + "/edit";
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

    private List<MediaDto> normalizeMedia(List<MediaDto> media) {
        if (media == null || media.isEmpty()) {
            return List.of();
        }

        List<MediaDto> normalized = new ArrayList<>();
        for (MediaDto item : media) {
            if (item != null && item.id() != null) {
                normalized.add(item);
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

        String contentType = file.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private String validateMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return "Изберете поне едно медийно изображение за качване.";
        }

        for (MultipartFile file : files) {
            if (file.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
                return "Всяко медийно изображение трябва да е 5 MB или по-малко. Изберете друг файл.";
            }

            if (!isImageFile(file)) {
                return "Медийните файлове трябва да са изображения.";
            }
        }

        return null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private <T> String firstValidationMessage(T value) {
        return validator.validate(value).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }

    private String resolveCurrentMainImageUrl(Long id) {
        try {
            ClubDto club = teacherClubClient.getManagedClubById(id);
            return nonNull(club.mainImageUrl());
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private List<MediaDto> resolveCurrentMedia(Long id) {
        try {
            ClubDto club = teacherClubClient.getManagedClubById(id);
            return normalizeMedia(club.media());
        } catch (RuntimeException ignored) {
            return List.of();
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
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Прегледайте данните за клуба и опитайте отново.";
            case UNAUTHORIZED, FORBIDDEN -> "Нямате право да управлявате този клуб.";
            case NOT_FOUND -> "Заявеният ресурс не е намерен.";
            default -> "Клубът не може да бъде запазен в момента. Опитайте отново.";
        };
    }

    private String toClubMediaErrorMessage(FeignException ex, boolean addOperation) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String fallback = switch (status) {
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> addOperation
                    ? "Прегледайте избраните изображения и опитайте отново."
                    : "Медийното изображение не може да бъде премахнато в момента.";
            case UNAUTHORIZED, FORBIDDEN -> "Нямате право да управлявате медийните изображения на този клуб.";
            case NOT_FOUND -> addOperation
                    ? "Клубът не беше намерен."
                    : "Клубът или медийното изображение не беше намерено.";
            default -> addOperation
                    ? "Медийните изображения не могат да бъдат качени в момента. Опитайте отново."
                    : "Медийното изображение не може да бъде премахнато в момента. Опитайте отново.";
        };

        return firstNonBlank(extractUserMessage(ex), fallback);
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
