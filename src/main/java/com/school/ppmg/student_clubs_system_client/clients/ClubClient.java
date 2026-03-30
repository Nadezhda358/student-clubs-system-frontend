package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.CreateClubRequest;
import com.school.ppmg.student_clubs_system_client.dtos.club.MediaDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpsertClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    ClubDto getById(@PathVariable Long id);

    @GetMapping("/{id}/events")
    List<Map<String, Object>> getEvents(@PathVariable Long id);

    @GetMapping("/{id}/media")
    List<MediaDto> getMedia(@PathVariable Long id);

    @GetMapping("/{id}/announcements")
    List<Map<String, Object>> getAnnouncements(@PathVariable Long id);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ClubDto create(@Valid @ModelAttribute CreateClubRequest request);

    @PutMapping("/{id}")
    ClubDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertClubDto dto
    );

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
