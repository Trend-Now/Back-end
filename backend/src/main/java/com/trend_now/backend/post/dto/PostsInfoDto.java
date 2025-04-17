/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.post.domain.Posts;
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
    private List<ImageInfoDto> imageInfos;

    public static PostsInfoDto of(Posts posts, List<ImageInfoDto> imageInfos) {
        return new PostsInfoDto(posts.getTitle(), posts.getWriter(), posts.getContent(),
                posts.getViewCount(), imageInfos);
    }
}
