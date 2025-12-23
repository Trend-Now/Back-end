package com.trend_now.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverNewsResponseDto {
    private List<NewsItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsItem {

        private String description;

        @Override
        public String toString() {
            return description.replaceAll("<[^>]*>", "");
        }
    }
}
