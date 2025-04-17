/*
 * 클래스 설명 : 게시글 관련 서비스
 * 메소드 설명
 * - findAllPostsPagingByBoardId() : 선택한 게시판의 게시글들을 페이징하여 조회한 후 반환
 * - savePosts() : 회원이 작성한 게시글을 게시판에 저장
 */
package com.trend_now.backend.post.application;

import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostListDto;
import com.trend_now.backend.post.dto.PostsDeleteDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateDto;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.List;
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
    private static final String NOT_SAME_WRITER = "작성자가 일치하지 않습니다.";

    private final PostsRepository postsRepository;
    private final BoardRepository boardRepository;
    private final ImagesService imagesService;
    private final PostLikesService postLikesService;

    // 게시판 조회 - 가변 타이머 작동 중에만 가능
    public Page<PostListDto> findAllPostsPagingByBoardId(
        PostsPagingRequestDto postsPagingRequestDto) {

        Pageable pageable = PageRequest.of(postsPagingRequestDto.getPage(),
            postsPagingRequestDto.getSize());

        // boardsId에 속하는 게시글 조회
        return postsRepository.findAllByBoardsId(
            postsPagingRequestDto.getBoardId(), pageable);
    }

    //게시글 작성 - 가변 타이머 작동 중에만 가능
    @Transactional
    public Long savePosts(PostsSaveDto postsSaveDto, Members members) {
        Boards findBoards = boardRepository.findById(postsSaveDto.getBoardId())
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_BOARD));

        Posts posts = Posts.builder()
            .title(postsSaveDto.getTitle())
            .content(postsSaveDto.getContent())
            .writer(members.getName())
            .members(members)
            .boards(findBoards)
            .build();
        Posts savePost = postsRepository.save(posts);

        // 저장돼 있던 이미지와 등록된 게시글 연관관계 설정
        if (postsSaveDto.getImageIds() != null) {
            postsSaveDto.getImageIds().forEach(
                imageId -> {
                    Images image = imagesService.findImageById(imageId);
                    image.setPosts(savePost);
                }
            );
        }
        return posts.getId();
    }

    //게시글 단건 조회 - 가변 타이머 작동 중에만 가능
    public PostsInfoDto findPostsById(Long postId) {
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));
        List<ImageInfoDto> imagesByPost = imagesService.findImagesByPost(posts);

        return PostsInfoDto.of(posts, imagesByPost);
    }

    //게시글 수정 - 가변 타이머 작동 중에만 가능
    @Transactional
    public void updatePostsById(PostsUpdateDto postsUpdateDto) {
        Posts posts = postsRepository.findById(postsUpdateDto.getPostId())
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        if (!posts.isSameWriter(postsUpdateDto.getWriter())) {
            throw new IllegalArgumentException(NOT_SAME_WRITER);
        }

        posts.changePosts(postsUpdateDto.getTitle(), postsUpdateDto.getContent());
    }

    //게시글 삭제 - 상시 가능
    @Transactional
    public void deletePostsById(PostsDeleteDto postsDeleteDto) {
        Posts posts = postsRepository.findById(postsDeleteDto.getPostId())
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        if (!posts.isSameWriter(postsDeleteDto.getWriter())) {
            throw new IllegalArgumentException(NOT_SAME_WRITER);
        }

        postsRepository.deleteById(postsDeleteDto.getPostId());
    }

    public Page<PostListDto> getPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posts> posts = postsRepository.findByMembers_Id(memberId, pageable);
        return posts.map(post -> {
            int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                post.getId());
            return PostListDto.of(post, postLikesCount);
        });
    }
}
