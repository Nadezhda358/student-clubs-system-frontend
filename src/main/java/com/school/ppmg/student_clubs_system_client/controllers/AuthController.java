package com.school.ppmg.student_clubs_system_client.controllers;

import com.school.ppmg.student_clubs_system_client.clients.AuthClient;
import com.school.ppmg.student_clubs_system_client.config.SessionConstants;
import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.dtos.auth.LoginRequest;
import com.school.ppmg.student_clubs_system_client.dtos.auth.LoginResponse;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterStudentRequest;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterTeacherRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthClient authClient;

    @GetMapping("/login")
    public String loginPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.SESSION_USER) != null) {
            return "redirect:/clubs";
        }

        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest("", ""));
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerStudentPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.SESSION_USER) != null) {
            return "redirect:/clubs";
        }

        if (!model.containsAttribute("registerStudentRequest")) {
            model.addAttribute(
                    "registerStudentRequest",
                    new RegisterStudentRequest("", "", "", "", null, "")
            );
        }

        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @Valid @ModelAttribute("registerStudentRequest") RegisterStudentRequest registerStudentRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.SESSION_USER) != null) {
            return "redirect:/clubs";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please complete all required fields.");
            return "register";
        }

        try {
            authClient.registerStudent(registerStudentRequest);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Registration complete. Please sign in to continue."
            );
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("error", "Unable to register. Please review your details and try again.");
            return "register";
        }
    }

    @GetMapping("/register/teacher")
    public String registerTeacherPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.SESSION_USER) != null) {
            return "redirect:/clubs";
        }

        if (!model.containsAttribute("registerTeacherRequest")) {
            model.addAttribute(
                    "registerTeacherRequest",
                    new RegisterTeacherRequest(token != null ? token : "", "", "", "")
            );
        }

        return "register-teacher";
    }

    @PostMapping("/register/teacher")
    public String registerTeacher(
            @Valid @ModelAttribute("registerTeacherRequest") RegisterTeacherRequest registerTeacherRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.SESSION_USER) != null) {
            return "redirect:/clubs";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please complete all required fields.");
            return "register-teacher";
        }

        try {
            authClient.registerTeacher(registerTeacherRequest);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Teacher registration complete. Please sign in to continue."
            );
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("error", "Unable to register with this invite token. Please try again.");
            return "register-teacher";
        }
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please enter a valid email and password.");
            return "login";
        }

        try {
            LoginResponse response = authClient.login(loginRequest);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                model.addAttribute("error", "Invalid login response. Please try again.");
                return "login";
            }

            AuthUserDto user = response.user();
            if (user == null) {
                model.addAttribute("error", "Invalid login response. Please try again.");
                return "login";
            }

            HttpSession session = request.getSession(true);
            session.setAttribute(SessionConstants.SESSION_JWT, response.accessToken());
            session.setAttribute(SessionConstants.SESSION_USER, user);

            String roleName = user.role() != null ? user.role().name() : "STUDENT";
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + roleName)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            return "redirect:/clubs";
        } catch (Exception ex) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SessionConstants.SESSION_JWT);
            session.removeAttribute(SessionConstants.SESSION_USER);
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }

        SecurityContextHolder.clearContext();
        return "redirect:/clubs";
    }
}
