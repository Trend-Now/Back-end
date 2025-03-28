/*
 * 클래스 설명 : 게시글 관련 서비스
 * 메소드 설명
 * - findAllPostsPagingByBoardId() : 선택한 게시판의 게시글들을 페이징하여 조회한 후 반환
 * - savePosts() : 회원이 작성한 게시글을 게시판에 저장
 */
package com.trend_now.backend.post.application;

import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostsService {

    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";
    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";

    private final PostsRepository postsRepository;
    private final BoardRepository boardRepository;

    public Page<PostsInfoDto> findAllPostsPagingByBoardId(
            PostsPagingRequestDto postsPagingRequestDto) {
        Pageable pageable = PageRequest.of(postsPagingRequestDto.getPage(),
                postsPagingRequestDto.getSize());

        Page<Posts> postsPaging = postsRepository.findAllByBoardsId(
                postsPagingRequestDto.getBoardId(), pageable);

        return postsPaging.map(PostsInfoDto::from);
    }

    public Long savePosts(PostsSaveDto postsSaveDto, Users users) {
        Boards findBoards = boardRepository.findById(postsSaveDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_BOARD));

        Posts posts = Posts.builder()
                .title(postsSaveDto.getTitle())
                .content(postsSaveDto.getContent())
                .writer(users.getName())
                .users(users)
                .boards(findBoards)
                .build();

        postsRepository.save(posts);
        return posts.getId();
    }

    public PostsInfoDto findPostsById(Long postId) {
        Posts posts = postsRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        return PostsInfoDto.from(posts);
    }
}
