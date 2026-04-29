package com.school.ppmg.student_clubs_system_client.dtos.event;

import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record UpsertEventDto(
        @NotNull Long clubId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull OffsetDateTime startAt,
        OffsetDateTime endAt,
        @Size(max = 200) String location,
        @Min(1) Integer capacity,
        OffsetDateTime registrationDeadline,
        @NotNull EventStatus status,
        @NotNull EventAudience audience
) {

    @AssertTrue(message = "Крайният час трябва да е след началния.")
    public boolean isEndAfterStart() {
        return endAt == null || startAt == null || !endAt.isBefore(startAt);
    }

    @AssertTrue(message = "Крайният срок за записване трябва да е на или преди началото.")
    public boolean isDeadlineValid() {
        return registrationDeadline == null || startAt == null || !registrationDeadline.isAfter(startAt);
    }
}
