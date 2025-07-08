/*
 * 클래스 설명 : 게시글 관련 서비스
 * 메소드 설명
 * - findAllPostsPagingByBoardId() : 선택한 게시판의 게시글들을 페이징하여 조회한 후 반환
 * - savePosts() : 회원이 작성한 게시글을 게시판에 저장
 */
package com.trend_now.backend.post.application;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateRequestDto;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostsService {

    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";
    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";
    private static final String NOT_SAME_WRITER = "작성자가 일치하지 않습니다.";
    private static final String NOT_REAL_TIME_BOARD = "타이머가 종료된 게시판입니다. 타이머가 남아있는 게시판에서만 요청할 수 있습니다.";
    private static final String NOT_MODIFIABLE_POSTS = "게시글이 수정/삭제 불가능한 상태입니다.";

    private final BoardRedisService boardRedisService;
    private final ImagesService imagesService;

    private final PostsRepository postsRepository;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final PostViewService postViewService;
    private final PostLikesService postLikesService;
    private final ScrapService scrapService;

    // 게시판 조회 - 가변 타이머 작동 중에만 가능
    public Page<PostSummaryDto> findAllPostsPagingByBoardId(
        PostsPagingRequestDto postsPagingRequestDto) {

        // 게시판이 가변 타이머가 작동 중인지 확인
        Boards boards = boardRepository.findById(postsPagingRequestDto.getBoardId()).
            orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));
        if (boardRedisService.isNotRealTimeBoard(boards.getName(), boards.getId(), boards.getBoardCategory())) {
            throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
        }

        Pageable pageable = PageRequest.of(postsPagingRequestDto.getPage(),
            postsPagingRequestDto.getSize(), Sort.by(Direction.DESC, "createdAt")); // 최신순으로 조회

        // boardsId에 속하는 게시글 조회
        Page<PostSummaryDto> postSummmaryPage = postsRepository.findAllByBoardsId(
            postsPagingRequestDto.getBoardId(), pageable);

        // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 PostSummaryDto에 설정 (Look Aside)
        postSummmaryPage.forEach(postSummaryDto -> {
            int postViewCount = postViewService.getPostViewCount(postSummaryDto.getPostId());
            postSummaryDto.setViewCount(postViewCount);
            int postLikesCount = postLikesService.getPostLikesCount(boards.getId(), postSummaryDto.getPostId());
            postSummaryDto.setLikeCount(postLikesCount);
        });

        return postSummmaryPage;
    }

    //게시글 단건 조회 - 가변 타이머 작동 중에만 가능
    public PostsInfoDto findPostsById(Long boardId, Long postId, Authentication authentication) {
        // 게시판이 가변 타이머가 작동 중인지 확인

        Boards boards = boardRepository.findById(boardId).
            orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        if (boardRedisService.isNotRealTimeBoard(boards.getName(), boards.getId(), boards.getBoardCategory())) {
            throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
        }


        // 게시글 정보 조회
        PostsInfoDto postsInfoDto = postsRepository.findPostInfoById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            // 인증 객체에 CustomUserDetails가 들어 있다면 로그인 한 회원
            Long requestMemberId = userDetails.getMembers().getId();
            // 현재 게시글에 요청한 Member가 스크랩을 했었는지 조회
            boolean isScraped = scrapService.isScrapedPost(requestMemberId, postId);
            postsInfoDto.setScraped(isScraped);
        } else {
            // 로그인하지 않은 사용자는 Scraped를 false로 설정
            postsInfoDto.setScraped(false);
        }

        // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 postsInfoDto에 설정 (Look Aside)
        int postViewCount = postViewService.getPostViewCount(postId);
        postsInfoDto.setViewCount(postViewCount + 1);
        int postLikesCount = postLikesService.getPostLikesCount(boards.getId(), postId);
        // 현재 조회된 조회수는 조회수 증가 전이므로, 조회수에 1을 더한 값을 응답 값으로 세팅
        postsInfoDto.setLikeCount(postLikesCount);

        // 조회 시 조회수 증가
        postViewService.incrementPostView(postId);

        return postsInfoDto;
    }

    //게시글 작성 - 가변 타이머 작동 중에만 가능
    @Transactional
    public Long savePosts(PostsSaveDto postsSaveDto, Members members, Long boardId) {
        Boards boards = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        // 게시판이 가변 타이머가 작동 중인지 확인
        if (boards.getBoardCategory() == BoardCategory.REALTIME) {
            if (boardRedisService.isNotRealTimeBoard(boards.getName(), boards.getId(), boards.getBoardCategory())) {
                throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
            }
        }

        Posts posts = Posts.builder()
            .title(postsSaveDto.getTitle())
            .writer(members.getName())
            .content(postsSaveDto.getContent())
            .boards(boards)
            .members(members)
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

    //게시글 수정 - 가변 타이머 작동 중에만 가능
    @Transactional
    public void updatePostsById(PostsUpdateRequestDto postsUpdateRequestDto, Long boardId,
        Long postId, Long memberId) {
        Boards boards = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));

        // 게시글이 수정 불가능한 상태면 예외 발생
        isModifiable(boards.getId(), boards.getName(), boards.getBoardCategory(), posts);

        // 게시글 작성자와 요청한 회원이 일치하지 않으면 예외 발생
        if (posts.isNotSameId(memberId)) {
            throw new IllegalArgumentException(NOT_SAME_WRITER);
        }

        // 삭제된 이미지 서버에서 삭제
        List<Long> deleteImageIdList = postsUpdateRequestDto.getDeleteImageIdList();
        if (deleteImageIdList != null && !deleteImageIdList.isEmpty()) {
            imagesService.deleteImageByIdList(deleteImageIdList);
        }
        // 새로 추가된 이미지 연관관계 설정
        List<Long> newImageIdList = postsUpdateRequestDto.getNewImageIdList();
        if (newImageIdList != null && !newImageIdList.isEmpty()) {
            newImageIdList.forEach(
                imageId -> {
                    Images image = imagesService.findImageById(imageId);
                    image.setPosts(posts);
                }
            );
        }
        // 제목, 내용 업데이트
        posts.changePosts(postsUpdateRequestDto.getTitle(), postsUpdateRequestDto.getContent());
    }

    //게시글 삭제 - 상시 가능
    @Transactional
    public void deletePostsById(Long boardId, Long postId, Long memberId) {
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));
        Boards boards = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        // 게시글이 수정 불가능한 상태면 예외 발생
        isModifiable(boards.getId(), boards.getName(), boards.getBoardCategory(), posts);

        if (posts.isNotSameId(memberId)) {
            throw new InvalidRequestException(NOT_SAME_WRITER);
        }

        // 게시글에 연관된 댓글 삭제
        commentsRepository.deleteByPosts_Id(postId);
        // 게시글에 등록된 이미지 삭제
        imagesService.deleteImageByPostId(posts.getId());
        // 게시글 삭제
        postsRepository.deleteById(postId);
    }

    // 회원이 작성한 게시글 조회 - 가변 타이머 작동 중에만 가능
    public Page<PostWithBoardSummaryDto> getPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
        return postsRepository.findByMemberId(memberId, pageable);
    }

    /**
     * 게시글이 속한 게시판의 타이머가 만료됐을 경우 modifiable 필드의 값을 false로 변경합니다. modifiable = false
     * 불가능
     */
    @Transactional
    public void updateModifiable(Long boardId) {
        postsRepository.updateFlagByBoardId(boardId);
    }

    private void isModifiable(Long boardId, String boardName, BoardCategory boardCategory, Posts posts) {
        if (boardRedisService.isNotRealTimeBoard(boardName, boardId, boardCategory)) {
            throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
        } else if (!posts.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_POSTS);
        }
    }
}
