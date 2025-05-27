package com.trend_now.backend.search.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AutoCompleteDto {

    private Long boardId;
    private String boardName;

}
