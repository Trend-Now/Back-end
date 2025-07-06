package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.ScrapAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScrapResponseDto {
    private String message;
    private ScrapAction scrapAction;

    public static ScrapResponseDto of(String message, ScrapAction scrapAction) {
        return new ScrapResponseDto(message, scrapAction);
    }
}
