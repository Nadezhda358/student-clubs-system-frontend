package com.school.ppmg.student_clubs_system_client;

import com.school.ppmg.student_clubs_system_client.dtos.announcement.UpsertAnnouncementDto;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterStudentRequest;
import com.school.ppmg.student_clubs_system_client.dtos.auth.RegisterTeacherRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.CreateClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ManageClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpsertClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.event.UpsertEventDto;
import com.school.ppmg.student_clubs_system_client.enums.DisplayText;
import com.school.ppmg.student_clubs_system_client.enums.EventAudience;
import com.school.ppmg.student_clubs_system_client.enums.EventStatus;
import com.school.ppmg.student_clubs_system_client.enums.MembershipRequestStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CyrillicFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCyrillicNamesInRegistrationForms() {
        assertValid(new RegisterStudentRequest(
                "ivan.petrov@example.com",
                "Парола123",
                "Иван",
                "Петров",
                10,
                "Б"
        ));

        assertValid(new RegisterTeacherRequest(
                "invite-token",
                "СигурнаПарола123",
                "Мария",
                "Иванова"
        ));
    }

    @Test
    void acceptsCyrillicClubFormContent() {
        assertValid(new CreateClubDto(
                "Клуб по роботика",
                "Работилница за ученици с интерес към роботи, програмиране и електроника.",
                "Всеки вторник от 15:30 ч.",
                "Кабинет 204",
                "robotika@example.com",
                "+359 888 123 456",
                true,
                List.of(1L, 2L)
        ));

        assertValid(new UpsertClubDto(
                "Клуб по литература",
                "Четем, обсъждаме и пишем кратки текстове на български език.",
                "Сряда след часовете",
                "Библиотека",
                "literatura@example.com",
                "0888 555 444",
                true
        ));

        assertValid(new ManageClubDto(
                "Математически клуб",
                "Подготовка за състезания и работа по интересни задачи.",
                "Петък, 14:30 ч.",
                "Кабинет 312",
                "math@example.com",
                "02 123 4567",
                true
        ));
    }

    @Test
    void acceptsCyrillicEventAndAnnouncementFormContent() {
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(7);

        assertValid(new UpsertEventDto(
                1L,
                "Състезание по роботика",
                "Екипите ще представят проектите си и ще демонстрират решенията.",
                startAt,
                startAt.plusHours(2),
                "Актова зала",
                30,
                startAt.minusDays(1),
                EventStatus.PUBLISHED,
                EventAudience.ALL_STUDENTS
        ));

        assertValid(new UpsertAnnouncementDto(
                1L,
                "Нова среща на клуба",
                "Следващата среща ще бъде в библиотеката. Носете лаптопи и идеи.",
                true
        ));
    }

    @Test
    void enumsKeepEnglishPersistedNamesAndExposeBulgarianDisplayText() {
        assertThat(EventStatus.PUBLISHED.name()).isEqualTo("PUBLISHED");
        assertThat(((DisplayText) EventStatus.PUBLISHED).getText()).isEqualTo("Публикувано");

        assertThat(MembershipRequestStatus.APPROVED.name()).isEqualTo("APPROVED");
        assertThat(((DisplayText) MembershipRequestStatus.APPROVED).getText()).isEqualTo("Одобрена");
    }

    private void assertValid(Object value) {
        Set<ConstraintViolation<Object>> violations = validator.validate(value);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .isEmpty();
    }
}
