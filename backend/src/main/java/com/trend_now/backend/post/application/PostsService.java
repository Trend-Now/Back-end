/*
 * 클래스 설명 : 게시글 관련 서비스
 * 메소드 설명
 * - findAllPostsPagingByBoardId() : 선택한 게시판의 게시글들을 페이징하여 조회한 후 반환
 * - savePosts() : 회원이 작성한 게시글을 게시판에 저장
 */
package com.trend_now.backend.post.application;

import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.RealTimeBoardCache;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostSearchResponseDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateRequestDto;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final PostsRepository postsRepository;
    private final BoardRepository boardRepository;
    private final ImagesService imagesService;
    private final PostLikesService postLikesService;
    private final RealTimeBoardCache realTimeBoardCache;

    // 게시판 조회 - 가변 타이머 작동 중에만 가능
    public List<PostSummaryDto> findAllPostsPagingByBoardId(PostsPagingRequestDto postsPagingRequestDto) {

        Pageable pageable = PageRequest.of(postsPagingRequestDto.getPage(),
            postsPagingRequestDto.getSize());

        // boardsId에 속하는 게시글 조회
        Page<Posts> postsPage = postsRepository.findAllByBoards_Id(postsPagingRequestDto.getBoardId(), pageable);
        // 게시글 목록을 PostSummaryDto 변환
        return postsPage.getContent().stream()
            .map(post -> {
                int postLikesCount = postLikesService.getPostLikesCount(postsPagingRequestDto.getBoardId(),
                    post.getId());
                return PostSummaryDto.of(post, postLikesCount);
            }).toList();
    }

    //게시글 단건 조회 - 가변 타이머 작동 중에만 가능
    public PostsInfoDto findPostsById(Long boardId, Long postId) {
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));
        List<ImageInfoDto> imagesByPost = imagesService.findImagesByPost(posts);
        int postLikesCount = postLikesService.getPostLikesCount(boardId, postId);

        return PostsInfoDto.of(posts, postLikesCount, imagesByPost);
    }

    public PostSearchResponseDto findRealTimeBoardByKeyword(String keyword, int page, int size) {
        // 현재 Redis에 저장된 실시간 인기 검색어 조회 (게시판 이름:게시글 ID) 형태로 되어 있음
        List<BoardCacheEntry> boardCacheEntryList = realTimeBoardCache.getBoardCacheEntryList();

        // 실시간 인기 검색어(게시판) 중, 키워드가 포함된 검색어(게시판)만 필터링
        List<BoardInfoDto> filteredKeywords = boardCacheEntryList.stream()
            .filter(boardEntry -> boardEntry.getBoardName().contains(keyword))
            .map(boardEntry -> BoardInfoDto.builder()
                .boardId(boardEntry.getBoardId())
                .boardName(boardEntry.getBoardName())
                .build())
            .sorted(Comparator.comparingLong(BoardInfoDto::getBoardLiveTime).reversed()
                .thenComparingDouble(BoardInfoDto::getScore))
            .toList();

        // 내용 또는 제목에 keyword가 포함된 게시글 중, 실시간 게시판에 해당하는(타이머가 남아있는) 게시글만 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Posts> posts = postsRepository.findByKeywordAndRealTimeBoard(keyword, realTimeBoardCache.getBoardCacheIdList(), pageable);
        List<PostSummaryDto> postSummaryDtoList = posts.getContent().stream()
            .map(post -> {
                int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                    post.getId());
                return PostSummaryDto.of(post, postLikesCount);
            }).toList();

        // 고정 게시판 조회
        List<Boards> fixedBoardList = boardRepository.findByNameLikeAndBoardCategory("%" + keyword + "%",
            BoardCategory.FIXED);
        List<BoardInfoDto> fixedBoardTitleList = fixedBoardList.stream().map(fixedBoard ->
                BoardInfoDto.builder()
                    .boardId(fixedBoard.getId())
                    .boardName(fixedBoard.getName())
                    .build())
            .toList();

        // 고정 게시판에서 게시글 조회
        Page<Posts> byFixedBoardAndKeyword = postsRepository.findByBoardCategoryAndKeyword(keyword, BoardCategory.FIXED, pageable);
        List<PostSummaryDto> fixedPostSummaryList = byFixedBoardAndKeyword.getContent().stream()
            .map(post -> {
                int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                    post.getId());
                return PostSummaryDto.of(post, postLikesCount);
            }).toList();

        return PostSearchResponseDto.builder()
            .message("게시글 검색 조회 성공")
            .boardTitleList(filteredKeywords)
            .postList(postSummaryDtoList)
            .fixedBoardTitleList(fixedBoardTitleList)
            .fixedPostList(fixedPostSummaryList)
            .build();
    }

    //게시글 작성 - 가변 타이머 작동 중에만 가능
    @Transactional
    public Long savePosts(PostsSaveDto postsSaveDto, Members members, Long boardId) {
        Boards findBoards = boardRepository.findById(boardId)
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

    //게시글 수정 - 가변 타이머 작동 중에만 가능
    @Transactional
    public void updatePostsById(PostsUpdateRequestDto postsUpdateRequestDto, Long postId, Long memberId) {
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

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
    public void deletePostsById(Long postId, Long memberId) {
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        if (posts.isNotSameId(memberId)) {
            throw new IllegalArgumentException(NOT_SAME_WRITER);
        }
        // TODO: 해당 게시글에 연관된 댓글 삭제
        // 게시글에 등록된 이미지 삭제
        imagesService.deleteImageByPostId(posts.getId());
        // 게시글 삭제
        postsRepository.deleteById(postId);
    }

    public List<PostSummaryDto> getPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posts> posts = postsRepository.findByMembers_Id(memberId, pageable);
        return posts.getContent().stream().map(post -> {
            int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                post.getId());
            return PostSummaryDto.of(post, postLikesCount);
        }).toList();
    }

}
