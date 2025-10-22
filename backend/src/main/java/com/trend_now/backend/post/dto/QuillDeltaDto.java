package com.trend_now.backend.post.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuillDeltaDto {

    private List<QuillOp> ops;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuillOp {
        private Object insert;
    }
}
