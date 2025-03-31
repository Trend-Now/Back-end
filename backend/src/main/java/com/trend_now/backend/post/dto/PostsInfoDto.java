/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.Posts;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsInfoDto {

    private String title;
    private String writer;
    private String content;
    private int viewCount;

    public static PostsInfoDto from(Posts posts) {
        return new PostsInfoDto(posts.getTitle(), posts.getWriter(), posts.getContent(),
                posts.getViewCount());
    }
}
