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
import com.trend_now.backend.exception.customException.InvalidRequestException;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.exception.customException.UnauthorizedException;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.CheckPostCooldownResponse;
import com.trend_now.backend.post.dto.PostInfoResponseDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateRequestDto;
import com.trend_now.backend.post.repository.PostLikesRepository;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.redis.core.RedisTemplate;
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
    private static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요한 서비스입니다.";

    private static final String POST_COOLDOWN_MESSAGE = "게시글 작성은 %s초 후에 가능합니다.";
    private static final String LAST_POST_TIME_KEY = "last_post_time";
    public static final String POST_COOLDOWN_PREFIX = "post-cooldown:";
    private static final String POST_COOLDOWN_KEY = "board:%s:user:%s";
    private static final long POST_LIMIT_SECONDS = 300;

    private final BoardRedisService boardRedisService;
    private final ImagesService imagesService;

    private final PostsRepository postsRepository;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final PostViewService postViewService;
    private final PostLikesService postLikesService;
    private final ScrapService scrapService;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final PostLikesRepository postLikesRepository;

    // 게시판 조회 - 가변 타이머 작동 중에만 가능
    public Page<PostSummaryDto> findAllPostsPagingByBoardId(
        PostsPagingRequestDto postsPagingRequestDto) {

        // 게시판이 가변 타이머가 작동 중인지 확인
//        Boards boards = findAndValidateBoard(postsPagingRequestDto.getBoardId());

        Boards boards = boardRepository.findById(postsPagingRequestDto.getBoardId())
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        Pageable pageable = PageRequest.of(postsPagingRequestDto.getPage(),
            postsPagingRequestDto.getSize(), Sort.by(Direction.DESC, "createdAt")); // 최신순으로 조회

        // boardsId에 속하는 게시글 조회
        Page<PostSummaryDto> postSummmaryPage = postsRepository.findAllByBoardsId(
            postsPagingRequestDto.getBoardId(), pageable);

        // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 PostSummaryDto에 설정 (Look Aside)
        postSummmaryPage.forEach(postSummaryDto -> {
            int postViewCount = postViewService.getPostViewCount(postSummaryDto.getPostId());
            postSummaryDto.setViewCount(postViewCount);
            int postLikesCount = postLikesService.getPostLikesCount(boards.getId(),
                postSummaryDto.getPostId());
            postSummaryDto.setLikeCount(postLikesCount);
        });

        return postSummmaryPage;
    }

    /**
     * 게시글 작성, 수정 API에 응답되는 메서드
     */
    public PostsInfoDto findPostsById(Long boardId, Long postId, Long requestMemberId, String memberName) {
        // 게시판이 존재하는지, 가변 타이머가 작동 중인지 확인
        Boards boards = findAndValidateBoard(boardId);

        // 게시글 정보 조회
        PostsInfoDto postsInfoDto = postsRepository.findPostInfoById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));

        // 현재 게시글이 내 게시글인지, 스크랩을 했던 게시글인지 조회
        setMyPostAndIsScrapAndIsLike(boardId, postId, requestMemberId, memberName, postsInfoDto);

        // 게시글 조회수와 게시글 좋아요 개수 값을 postsInfoDto에 설정 (Look Aside)
        setViewCountAndPostLike(postId, postsInfoDto, boards);
        postsInfoDto.setViewCount(0); // 게시글 작성, 수정 API에서는 조회수 0으로 강제 세팅

        return postsInfoDto;
    }

    /**
     * 게시글 단건 조회 - 가변 타이머 작동 중에만 가능
     * 게시글 상세 조회 API에 응답되는 메서드
     */
    public PostInfoResponseDto findPostsById(Long boardId, Long postId, Authentication authentication) {
        Boards boards = boardRepository.findById(boardId).
            orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        // 게시판이 존재하는지, 가변 타이머가 작동 중인지 확인
//        Boards boards = findAndValidateBoard(boardId);

        // 게시글 정보 조회
        PostsInfoDto postsInfoDto = postsRepository.findPostInfoById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            // 인증 객체에 CustomUserDetails가 들어 있다면 로그인 한 회원
            Long requestMemberId = userDetails.getMembers().getId();
            String requestMemberName = userDetails.getMembers().getName();
            // 현재 게시글에 요청한 Member가 스크랩을 했었는지 조회
            setMyPostAndIsScrapAndIsLike(boardId, postId, requestMemberId, requestMemberName, postsInfoDto);
        } else {
            // 로그인하지 않은 사용자는 isScraped와 isMyPost를 false로 설정
            postsInfoDto.setMyPost(false);
            postsInfoDto.setScraped(false);
            postsInfoDto.setLiked(false);
        }

        // 게시글 조회수와 게시글 좋아요 개수 값을 postsInfoDto에 설정 (Look Aside)
        setViewCountAndPostLike(postId, postsInfoDto, boards);

        // 조회 시 조회수 증가
        postViewService.incrementPostView(postId);

        List<ImageInfoDto> imagesByPost = imagesService.findImagesByPost(postId);

        return PostInfoResponseDto.of(postsInfoDto, imagesByPost);
    }

    @NotNull
    private Boards findAndValidateBoard(Long boardId) {
        Boards boards = boardRepository.findById(boardId).
            orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        if (boardRedisService.isNotRealTimeBoard(boards.getName(), boards.getId(),
            boards.getBoardCategory())) {
            throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
        }
        return boards;
    }

    private void setViewCountAndPostLike(Long postId, PostsInfoDto postsInfoDto, Boards boards) {
        int postViewCount = postViewService.getPostViewCount(postId);
        postsInfoDto.setViewCount(postViewCount + 1);
        int postLikesCount = postLikesService.getPostLikesCount(boards.getId(), postId);
        // 현재 조회된 조회수는 조회수 증가 전이므로, 조회수에 1을 더한 값을 응답 값으로 세팅
        postsInfoDto.setLikeCount(postLikesCount);
    }

    private void setMyPostAndIsScrapAndIsLike(Long boardId, Long postId, Long requestMemberId, String memberName, PostsInfoDto postsInfoDto) {
        boolean isMyPost = postsInfoDto.getWriterId().equals(requestMemberId);
        boolean isScraped = scrapService.isScrapedPost(requestMemberId, postId);
        boolean isLiked = postLikesService.hasMemberLiked(boardId, postId, memberName);
        postsInfoDto.setMyPost(isMyPost);
        postsInfoDto.setScraped(isScraped);
        postsInfoDto.setLiked(isLiked);
    }
    //게시글 단건 조회 - 가변 타이머 작동 중에만 가능

    /**
     * 게시글 작성 시, 같은 사용자가 같은 게시판에서 5분 이내에 게시글을 작성할 수 있는지 확인하는 메서드
     */
    public CheckPostCooldownResponse checkPostCooldown(Long boardId, Object principal) {
        // 로그인하지 않은 사용자라면 예외 발생
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException(LOGIN_REQUIRED_MESSAGE);
        }
        Long memberId = userDetails.getMembers().getId();
        String boardUserKey = String.format(POST_COOLDOWN_PREFIX + POST_COOLDOWN_KEY, boardId,
            memberId);
        long postCoolDown = getPostCooldown(boardUserKey);
        // 같은 사용자가 같은 게시판에서의 cooldown이 남아있는지 확인
        if (postCoolDown > 0) {
            return CheckPostCooldownResponse.of(false, postCoolDown);
        }
        return CheckPostCooldownResponse.of(true, 0L);
    }

    //게시글 작성 - 가변 타이머 작동 중에만 가능
    @Transactional
    public PostInfoResponseDto savePosts(PostsSaveDto postsSaveDto, Members members, Long boardId) {
        Boards boards = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        // 게시판이 가변 타이머가 작동 중인지 확인
        if (boards.getBoardCategory() == BoardCategory.REALTIME) {
            if (boardRedisService.isNotRealTimeBoard(boards.getName(), boards.getId(),
                boards.getBoardCategory())) {
                throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
            }
        }

        String boardUserKey = String.format(POST_COOLDOWN_PREFIX + POST_COOLDOWN_KEY, boardId,
            members.getId());
//        long postCoolDown = getPostCooldown(boardUserKey);
//        // 같은 사용자가 같은 게시판에서의 cooldown이 남아있는지 확인
//        if (postCoolDown > 0) {
//            throw new IllegalStateException(String.format(POST_COOLDOWN_MESSAGE, postCoolDown));
//        }
        refreshPostLimit(boardUserKey);

        Posts posts = Posts.builder()
            .title(postsSaveDto.getTitle())
            .writer(members.getName())
            .content(postsSaveDto.getContent())
            .boards(boards)
            .members(members)
            .build();

        boardRedisService.updatePostCountAndExpireTime(boards.getId(), boards.getName());
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
        PostsInfoDto postsInfoDto = findPostsById(boardId, savePost.getId(), members.getId(), members.getName());
        List<ImageInfoDto> imageInfoDtoList = imagesService.findImagesByPost(savePost.getId());
        return PostInfoResponseDto.of(postsInfoDto, imageInfoDtoList);
    }

    private void refreshPostLimit(String boardUserKey) {
        // 게시글 작성 시, 마지막 게시글 작성 시간을 현재 시간으로 갱신
        redisTemplate.opsForHash()
            .put(boardUserKey, LAST_POST_TIME_KEY, System.currentTimeMillis());
        redisTemplate.expire(boardUserKey, POST_LIMIT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 게시글 작성 시, 같은 사용자가 같은 게시판에서 5분 이내에 게시글을 작성할 수 없도록 제한하는 메서드
     */
    private long getPostCooldown(String boardUserKey) {
        Long lastPostTime = (Long) redisTemplate.opsForHash().get(boardUserKey, LAST_POST_TIME_KEY);
        if (lastPostTime != null) {
            // 마지막으로 작성된 시간으로부터 경과된 시간
            long elapsedTime = (System.currentTimeMillis() - lastPostTime) / 1000;
            return POST_LIMIT_SECONDS - elapsedTime;
        }
        return 0L; // redis에 값이 존재하지 않는 경우, 0초를 반환하여 게시글 작성이 가능함을 알림
    }

    //게시글 수정 - 가변 타이머 작동 중에만 가능
    @Transactional
    public PostInfoResponseDto updatePostsById(PostsUpdateRequestDto postsUpdateRequestDto, Long boardId,
        Long postId, Long memberId, String memberName) {
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

        // 제목, 내용 업데이트
        posts.changePosts(postsUpdateRequestDto.getTitle(), postsUpdateRequestDto.getContent());
        // 이미지 삭제 벌크 연산 이후 1차 캐시가 초기화 되기 떄문에 강제로 flush
        postsRepository.flush();

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

        // 응답 생성
        PostsInfoDto postsInfoDto = findPostsById(boardId, posts.getId(), memberId, memberName);
        List<ImageInfoDto> imageInfoDtoList = imagesService.findImagesByPost(posts.getId());

        return PostInfoResponseDto.of(postsInfoDto, imageInfoDtoList);
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

        boardRedisService.decrementPostCountAndExpireTime(posts.getBoards().getId(),
            posts.getBoards().getName());
        // 게시글 삭제
        postsRepository.deleteById(postId);
    }

    // 회원이 작성한 게시글 조회 - 가변 타이머 작동 중에만 가능
    public Page<PostWithBoardSummaryDto> getPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
        return postsRepository.findByMemberId(memberId, pageable);
    }

    /**
     * 게시글이 속한 게시판의 타이머가 만료됐을 경우 modifiable 필드의 값을 false로 변경합니다. modifiable = false 불가능
     */
    @Transactional
    public void updateModifiable(Long boardId) {
        postsRepository.updateFlagByBoardId(boardId);
    }

    private void isModifiable(Long boardId, String boardName, BoardCategory boardCategory,
        Posts posts) {
        if (boardRedisService.isNotRealTimeBoard(boardName, boardId, boardCategory)) {
            throw new InvalidRequestException(NOT_REAL_TIME_BOARD);
        } else if (!posts.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_POSTS);
        }
    }
}
