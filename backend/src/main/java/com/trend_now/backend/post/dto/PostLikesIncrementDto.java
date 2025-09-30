package com.trend_now.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLikesIncrementDto {

    private String memberName;
    private Long boardId;
    private Long postId;

    public static PostLikesIncrementDto of(String memberName, Long boardId, Long postId) {
        return new PostLikesIncrementDto(memberName, boardId, postId);
    }
}
