package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.*;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "student-clubs-system",
        contextId = "clubClient",
        url = "${app.api.base-url}/api/clubs"
)
public interface ClubClient {

    @GetMapping
    PageResponse<ClubListDto> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    ClubDto getById(@PathVariable Long id);

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ClubDto create(@Valid @RequestBody CreateClubDto dto);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ClubDto createMultipart(@Valid @ModelAttribute CreateClubRequest request);

    @PutMapping("/{id}")
    ClubDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertClubDto dto
    );

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);

    @PostMapping("/{id}/teachers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addTeachers(
            @PathVariable Long id,
            @Valid @RequestBody AddClubTeachersRequest request
    );

    @DeleteMapping("/{id}/teachers/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeTeacher(
            @PathVariable Long id,
            @PathVariable Long teacherId
    );

    @PostMapping(value = "/{id}/main-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ClubDto uploadMainImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    );
}
