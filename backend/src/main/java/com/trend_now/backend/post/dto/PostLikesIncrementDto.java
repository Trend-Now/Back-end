package com.trend_now.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLikesIncrementDto {

    private String memberName;
    private String boardName;
    private Long boardId;
    private Long postId;

    public static PostLikesIncrementDto of(String memberName, String boardName, Long boardId, Long postId) {
        return new PostLikesIncrementDto(memberName, boardName, boardId, postId);
    }
}
