package com.trend_now.backend.thread.presentation;

import com.trend_now.backend.global.dto.ApiResponse;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.thread.application.ThreadsService;
import com.trend_now.backend.thread.dto.ThreadsSaveDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/{boardId}/posts/{postId}/threads")
@Tag(name = "Thread API", description = "쓰레드 관련 API")
public class ThreadsController {

    private static final String SUCCESS_SAVE_THREADS_MESSAGE = "쓰레드 저장에 성공했습니다.";

    private final ThreadsService threadsService;

    @Operation(summary = "쓰레드 저장", description = "쓰레드를 저장합니다.")
    @PostMapping("/")
    public ResponseEntity<ApiResponse<Void>> saveThreads(
            @Valid @RequestBody ThreadsSaveDto threadsSaveDto,
            @PathVariable(value = "postId") Long postId,
            @AuthenticationPrincipal(expression = "members") Members members) {

        threadsService.saveThreads(threadsSaveDto, postId, members);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(null, SUCCESS_SAVE_THREADS_MESSAGE));
    }
}
