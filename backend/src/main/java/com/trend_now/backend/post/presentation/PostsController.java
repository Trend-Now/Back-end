package com.trend_now.backend.post.presentation;

import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.dto.PostsDeleteDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsPagingResponseDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateDto;
import com.trend_now.backend.user.domain.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "Post API", description = "게시글 관련 API")
public class PostsController {

    private static final String SUCCESS_PAGING_POSTS_MESSAGE = "모든 게시글을 가져오는 데 성공했습니다.";
    private static final String SUCCESS_SAVE_POSTS_MESSAGE = "게시글을 저장하는 데 성공했습니다.";
    private static final String SUCCESS_UPDATE_POSTS_MESSAGE = "게시글을 수정하는 데 성공했습니다.";
    private static final String SUCCESS_DELETE_POSTS_MESSAGE = "게시글을 삭제하는 데 성공했습니다.";

    private final PostsService postsService;

    @Operation(summary = "게시글 조회", description = "게시판의 모든 게시글을 페이징하여 조회합니다.")
    @GetMapping("/list/{postId}")
    public ResponseEntity<PostsPagingResponseDto> findAllPostsByBoardId(@RequestParam Long boardId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        PostsPagingRequestDto postsPagingRequestDto = new PostsPagingRequestDto(boardId, page,
                size);

        Page<PostsInfoDto> allPostsPagingByBoardId = postsService.findAllPostsPagingByBoardId(
                postsPagingRequestDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(PostsPagingResponseDto.of(SUCCESS_PAGING_POSTS_MESSAGE,
                        allPostsPagingByBoardId.getContent()));
    }

    @Operation(summary = "게시글 저장", description = "게시판에 게시글을 저장합니다.")
    @PostMapping("/")
    public ResponseEntity<String> savePosts(@Valid @RequestBody PostsSaveDto postsSaveDto,
            Users users) {

        Long savePosts = postsService.savePosts(postsSaveDto, users);

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_SAVE_POSTS_MESSAGE);
    }

    @Operation(summary = "게시글 수정", description = "게시판에 게시글의 제목 또는 내용을 수정합니다.")
    @PutMapping("/")
    public ResponseEntity<String> updatePosts(@Valid @RequestBody PostsUpdateDto postsUpdateDto) {

        postsService.updatePostsById(postsUpdateDto);

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_UPDATE_POSTS_MESSAGE);
    }

    @Operation(summary = "게시글 삭제", description = "작성한 게시글을 삭제합니다.")
    @DeleteMapping("/")
    public ResponseEntity<String> deletePosts(@Valid @RequestBody PostsDeleteDto postsDeleteDto) {

        postsService.deletePostsById(postsDeleteDto);

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_DELETE_POSTS_MESSAGE);
    }
}
