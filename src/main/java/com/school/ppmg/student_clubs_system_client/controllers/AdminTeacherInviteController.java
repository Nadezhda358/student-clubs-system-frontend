package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.TeacherInviteClient;
import com.school.ppmg.student_clubs_system_client.dtos.teacher.TeacherInviteBulkRequest;
import com.school.ppmg.student_clubs_system_client.dtos.teacher.TeacherInviteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class AdminTeacherInviteController {
    private final TeacherInviteClient teacherInviteClient;

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/clubs";
    }

    @GetMapping({"/admin/teacher-invites", "/admin/teacher-invites/new"})
    public String newTeacherInvitesPage(Model model) {
        if (!model.containsAttribute("emailsText")) {
            model.addAttribute("emailsText", "");
        }

        return "admin/teacher-invites-new";
    }

    @PostMapping("/admin/teacher-invites")
    public String createTeacherInvites(
            @RequestParam(value = "emailsText", required = false) String emailsText,
            Model model
    ) {
        List<String> emails = parseEmails(emailsText);
        model.addAttribute("emailsText", emailsText != null ? emailsText : "");

        if (emails.isEmpty()) {
            model.addAttribute("error", "Please provide at least one email address.");
            return "admin/teacher-invites-new";
        }

        try {
            List<TeacherInviteResponse> invites = teacherInviteClient.createTeacherInvites(
                    new TeacherInviteBulkRequest(emails)
            );

            model.addAttribute("invites", invites);
            model.addAttribute("success", "Invites sent: " + invites.size());

            if (invites.size() < emails.size()) {
                model.addAttribute(
                        "warning",
                        "Some emails were skipped because they already have accounts."
                );
            }

            model.addAttribute("emailsText", "");
            return "admin/teacher-invites-new";
        } catch (Exception ex) {
            model.addAttribute("error", friendlyErrorMessage(ex));
            return "admin/teacher-invites-new";
        }
    }

    private List<String> parseEmails(String emailsText) {
        if (emailsText == null || emailsText.isBlank()) {
            return List.of();
        }

        String[] tokens = emailsText.split("[\\s,;]+");
        LinkedHashSet<String> unique = new LinkedHashSet<>();

        for (String token : tokens) {
            if (token == null) {
                continue;
            }

            String cleaned = token.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isBlank()) {
                unique.add(cleaned);
            }
        }

        return new ArrayList<>(unique);
    }

    private String friendlyErrorMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        String lowered = message.toLowerCase(Locale.ROOT);

        if (lowered.contains("409") || lowered.contains("conflict")) {
            return "No invites were created. Those emails may already be registered.";
        }

        if (lowered.contains("bad request") || lowered.contains("400")) {
            return "Some email addresses look invalid. Please review and try again.";
        }

        return "Unable to send invites right now. Please try again.";
    }
}
