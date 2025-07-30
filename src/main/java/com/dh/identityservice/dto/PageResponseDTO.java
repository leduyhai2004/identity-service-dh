package com.dh.identityservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResponseDTO<T> {
    private List<T> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
}

