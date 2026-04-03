package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ManageClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "student-clubs-system",
        contextId = "teacherClubClient",
        url = "${app.api.base-url}/api/teacher/clubs"
)
public interface TeacherClubClient {

    @GetMapping
    PageResponse<ClubListDto> getManagedClubs(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    ClubDto getManagedClubById(@PathVariable Long id);

    @PutMapping("/{id}")
    ClubDto updateManagedClub(
            @PathVariable Long id,
            @Valid @RequestBody ManageClubDto dto
    );
    @PostMapping(value = "/{id}/main-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ClubDto uploadManagedClubMainImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    );
}
