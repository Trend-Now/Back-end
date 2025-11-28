package com.trend_now.backend.opensearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenSearchDocumentDto {
    private Long boardId;
    private String keyword;
}
