package com.school.ppmg.student_clubs_system_client.controllers.support;

import com.school.ppmg.student_clubs_system_client.dtos.auth.AuthUserDto;
import com.school.ppmg.student_clubs_system_client.enums.RegistrationStatus;
import com.school.ppmg.student_clubs_system_client.enums.UserRole;
import feign.FeignException;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class EventViewSupport {
    public static final int BROWSER_PAGE_SIZE = 12;
    public static final int TAB_PAGE_SIZE = 24;
    public static final int PARTICIPANTS_PAGE_SIZE = 20;
    public static final String EVENT_SORT = "startAt,asc";
    public static final String PARTICIPATION_SORT = "registeredAt,desc";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Sofia");
    private static final DateTimeFormatter FORM_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private EventViewSupport() {
    }

    public static boolean isStudent(AuthUserDto user) {
        return user != null && user.role() == UserRole.STUDENT;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static RegistrationStatus parseRegistrationStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            return RegistrationStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static OffsetDateTime parseDateTimeInput(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(normalized, FORM_DATE_TIME);
            return localDateTime.atZone(BUSINESS_ZONE).toOffsetDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static OffsetDateTime parseFromDate(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(normalized);
            return date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static OffsetDateTime parseToDate(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(normalized);
            return date.atTime(LocalTime.MAX).atZone(BUSINESS_ZONE).toOffsetDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static String toDateInput(OffsetDateTime value) {
        if (value == null) {
            return "";
        }

        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDate().toString();
    }

    public static String toDateTimeInput(OffsetDateTime value) {
        if (value == null) {
            return "";
        }

        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime().format(FORM_DATE_TIME);
    }

    public static String formatDateTime(OffsetDateTime value) {
        if (value == null) {
            return "";
        }

        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime().format(DISPLAY_DATE_TIME);
    }

    public static HttpStatus resolveStatus(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        return status == null ? HttpStatus.BAD_GATEWAY : status;
    }

    public static String extractUserMessage(FeignException ex) {
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

    public static String formatLabel(Enum<?> value) {
        if (value == null) {
            return "";
        }

        String normalized = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    public static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    public static String extractJsonField(String json, String fieldName) {
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
