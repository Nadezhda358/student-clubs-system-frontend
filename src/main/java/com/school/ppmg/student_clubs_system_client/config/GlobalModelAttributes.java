package com.school.ppmg.student_clubs_system_client.config;

import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

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

    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            MultipartException.class,
            FileSizeLimitExceededException.class
    })
    public String handleUploadTooLarge(HttpServletRequest request, HttpServletResponse response) {
        String targetUrl = request.getHeader("Referer");
        if (targetUrl == null || targetUrl.isBlank()) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/admin/clubs/") && uri.endsWith("/main-image")) {
                targetUrl = uri.substring(0, uri.length() - "/main-image".length()) + "/edit";
            } else if (uri != null && uri.startsWith("/teacher/clubs/") && uri.endsWith("/main-image")) {
                targetUrl = uri.substring(0, uri.length() - "/main-image".length()) + "/edit";
            } else if ("/admin/events/create".equals(uri)) {
                targetUrl = "/admin/events/create";
            } else if (uri != null && uri.startsWith("/admin/events/") && uri.endsWith("/edit")) {
                targetUrl = uri;
            } else if ("/teacher/events/create".equals(uri)) {
                targetUrl = "/teacher/events/create";
            } else if (uri != null && uri.startsWith("/teacher/events/") && uri.endsWith("/edit")) {
                targetUrl = uri;
            } else if ("/admin/clubs/create".equals(uri)) {
                targetUrl = "/admin/clubs/create";
            } else {
                targetUrl = "/clubs";
            }
        }

        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("errorMessage", "Uploaded file is too large. Maximum size is 5 MB per file.");
        RequestContextUtils.saveOutputFlashMap(targetUrl, request, response);
        return "redirect:" + targetUrl;
    }
}
