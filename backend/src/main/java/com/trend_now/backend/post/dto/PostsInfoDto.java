/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.post.domain.Posts;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostsInfoDto {

    private final String title;
    private final String writer;
    private final String content;
    private final int viewCount;
    private final int likeCount;
    private final boolean modifiable;
    private final List<ImageInfoDto> imageInfos;
    private final LocalDateTime updatedAt;

    public static PostsInfoDto of(Posts posts, int likeCount, List<ImageInfoDto> imageInfos) {
        return new PostsInfoDto(posts.getTitle(), posts.getWriter(), posts.getContent(),
                posts.getViewCount(), likeCount, posts.isModifiable(), imageInfos, posts.getUpdatedAt());
    }
}
