package com.trend_now.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SearchResponseDto {

    private String message;
    private Object searchResult;

    public static SearchResponseDto of(String message, Object data) {
        return new SearchResponseDto(message, data);
    }
}
