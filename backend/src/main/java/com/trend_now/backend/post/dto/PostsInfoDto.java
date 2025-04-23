/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.post.domain.Posts;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PostsInfoDto {

    private String title;
    private String writer;
    private String content;
    private int viewCount;
    private int likeCount;
    private List<ImageInfoDto> imageInfos;
    private LocalDateTime updatedAt;

    public static PostsInfoDto of(Posts posts, int likeCount, List<ImageInfoDto> imageInfos) {
        return new PostsInfoDto(posts.getTitle(), posts.getWriter(), posts.getContent(),
                posts.getViewCount(), likeCount, imageInfos, posts.getUpdatedAt());
    }
}
