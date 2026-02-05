package com.school.ppmg.student_clubs_system_client.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AppConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs == null) {
                return;
            }

            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }

            Object token = session.getAttribute(SessionConstants.SESSION_JWT);
            if (token instanceof String jwt && !jwt.isBlank()) {
                requestTemplate.header("Authorization", "Bearer " + jwt);
            }
        };
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> switch (response.status()) {
            case 400 -> new RuntimeException("Bad request");
            case 401 -> new RuntimeException("Unauthorized");
            case 403 -> new RuntimeException("Forbidden");
            case 404 -> new RuntimeException("Not found");
            case 500 -> new RuntimeException("Backend error");
            default -> new RuntimeException("Feign error: " + response.status());
        };
    }
}
