package com.school.ppmg.student_clubs_system_client.config;

import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("sessionUser")
    public AuthUserDto sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object user = session.getAttribute(SessionConstants.SESSION_USER);
        if (user instanceof AuthUserDto authUser) {
            return authUser;
        }

        return null;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (uri == null || uri.isBlank()) {
            return "/";
        }

        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            String resolved = uri.substring(contextPath.length());
            return resolved.isBlank() ? "/" : resolved;
        }

        return uri;
    }
}
