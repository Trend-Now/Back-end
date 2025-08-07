package com.trend_now.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckPostCooldownResponse {
    private boolean canWritePost;
    private Long cooldownSeconds;

    public static CheckPostCooldownResponse of(boolean canWritePost, Long cooldownSeconds) {
        return new CheckPostCooldownResponse(canWritePost, cooldownSeconds);
    }
}
