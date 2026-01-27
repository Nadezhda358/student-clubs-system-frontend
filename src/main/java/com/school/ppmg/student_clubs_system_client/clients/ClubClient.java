package com.school.ppmg.student_clubs_system_client.clients;

import com.school.ppmg.student_clubs_system_client.dtos.club.ClubListDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.ClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.club.UpsertClubDto;
import com.school.ppmg.student_clubs_system_client.dtos.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "student-clubs-system",
        contextId = "clubClient",
        url = "${backend.api.base-url}/api/clubs"
)
public interface ClubClient {

    @GetMapping
    PageResponse<ClubListDto> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping("/{id}")
    ClubDto getById(@PathVariable Long id);

    @PostMapping
    ClubDto create(@Valid @RequestBody UpsertClubDto dto);

    @PutMapping("/{id}")
    ClubDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertClubDto dto
    );

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
