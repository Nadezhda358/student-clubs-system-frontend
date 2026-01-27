package com.school.ppmg.student_clubs_system_client.dtos.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;

    private int number;          // current page (0-based)
    private int size;            // requested size
    private int numberOfElements;

    private long totalElements;
    private int totalPages;

    private boolean first;
    private boolean last;
    private boolean empty;
}
