package com.school.ppmg.student_clubs_system_client.dtos.club;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class CreateClubRequest {

    @NotBlank
    @Size(max = 160)
    private String name;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @Size(max = 2000)
    private String scheduleText;

    @Size(max = 80)
    private String room;

    @Email
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 40)
    private String contactPhone;

    @NotNull
    private Boolean isActive;

    @Positive
    private Long teacherId;

    private Boolean teacherIsPrimary;

    private MultipartFile mainImage;

    private List<MultipartFile> mediaFiles;

    public UpsertClubDto toUpsertDto() {
        return new UpsertClubDto(
                name,
                description,
                scheduleText,
                room,
                contactEmail,
                contactPhone,
                isActive
        );
    }
}
