package com.trend_now.backend.post.presentation;

import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.dto.CheckPostCooldownResponse;
import com.trend_now.backend.post.dto.PostInfoResponseDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostListResponseDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/{boardId}")
@Tag(name = "Post API", description = "게시글 관련 API")
public class PostsController {

    private static final String SUCCESS_PAGING_POSTS_MESSAGE = "모든 게시글을 가져오는 데 성공했습니다.";
    private static final String SUCCESS_FIND_POSTS_MESSAGE = "게시글을 가져오는 데 성공했습니다.";
    private static final String SUCCESS_SAVE_POSTS_MESSAGE = "게시글을 저장하는 데 성공했습니다.";
    private static final String SUCCESS_UPDATE_POSTS_MESSAGE = "게시글을 수정하는 데 성공했습니다.";
    private static final String SUCCESS_DELETE_POSTS_MESSAGE = "게시글을 삭제하는 데 성공했습니다.";

    private final PostsService postsService;
    private final ImagesService imagesService;
    private final BoardService boardService;

    @Operation(summary = "게시글 목록 조회", description = "게시판의 모든 게시글을 페이징하여 조회합니다.")
    @GetMapping("/posts")
    public ResponseEntity<PostListResponseDto> findAllPostsByBoardId(
        @PathVariable Long boardId,
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestParam(required = false, defaultValue = "10") int size) {

        PostsPagingRequestDto postsPagingRequestDto = PostsPagingRequestDto.of(boardId, page - 1,
            size);
        String boardName = boardService.getBoardNameById(boardId);
        Page<PostSummaryDto> postSummaryDtoPage = postsService.findAllPostsPagingByBoardId(
            postsPagingRequestDto);

        return ResponseEntity.status(HttpStatus.OK)
            .body(PostListResponseDto.of(SUCCESS_PAGING_POSTS_MESSAGE,
                postSummaryDtoPage.getTotalPages(), postSummaryDtoPage.getTotalElements(),
                boardName, postSummaryDtoPage.getContent()));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시판의 게시글을 상세 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostInfoResponseDto> findPostById(
        @PathVariable(value = "boardId") Long boardId,
        @PathVariable(value = "postId") Long postId
    ) {
        // 게시글 스크랩 여부를 확인하기 위해 인증 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PostsInfoDto postInfo = postsService.findPostsById(boardId, postId, authentication);
        List<ImageInfoDto> imagesByPost = imagesService.findImagesByPost(postId);

        return ResponseEntity.status(HttpStatus.OK)
            .body(PostInfoResponseDto.of(SUCCESS_FIND_POSTS_MESSAGE, postInfo, imagesByPost));
    }

    @Operation(summary = "게시글 저장", description = "게시판에 게시글을 저장합니다.")
    @PostMapping("/posts")
    public ResponseEntity<String> savePosts(@Valid @RequestBody PostsSaveDto postsSaveDto,
        @PathVariable(value = "boardId") Long boardId,
        @AuthenticationPrincipal(expression = "members") Members members) {

        postsService.savePosts(postsSaveDto, members, boardId);

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_SAVE_POSTS_MESSAGE);
    }

    @Operation(summary = "게시글 수정", description = "게시판에 게시글의 제목 또는 내용을 수정합니다.")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<String> updatePosts(
        @Valid @RequestBody PostsUpdateRequestDto postsUpdateRequestDto,
        @PathVariable(value = "boardId") Long boardId,
        @PathVariable(value = "postId") Long postId,
        @AuthenticationPrincipal(expression = "members") Members members) {

        postsService.updatePostsById(postsUpdateRequestDto, boardId, postId, members.getId());

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_UPDATE_POSTS_MESSAGE);
    }

    @Operation(summary = "게시글 삭제", description = "작성한 게시글을 삭제합니다.")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<String> deletePosts(
        @PathVariable(value = "boardId") Long boardId,
        @PathVariable(value = "postId") Long postId,
        @AuthenticationPrincipal(expression = "members") Members members
    ) {

        postsService.deletePostsById(boardId, postId, members.getId());

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_DELETE_POSTS_MESSAGE);
    }

    @Operation(summary = "게시판 글쓰기 가능 여부 확인", description = "요청한 사용자가 현재 게시판에 게시글을 작성한 후 5분 이내에는 추가 작성이 불가능하도록 쿨다운 상태를 확인합니다.")
    @GetMapping("/posts/cooldown")
    public ResponseEntity<CheckPostCooldownResponse> checkPostCooldown(
        @PathVariable(value = "boardId") Long boardId,
        @AuthenticationPrincipal(expression = "members") Members members) {

        return ResponseEntity.status(HttpStatus.OK)
            .body(postsService.checkPostCooldown(boardId, members.getId()));
    }
}