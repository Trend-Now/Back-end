package com.trend_now.backend.comment.application;

import com.trend_now.backend.board.application.BoardKeyProvider;
import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.*;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardExpiredException;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentsService {

    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";
    private static final String NOT_EXIST_COMMENTS = "댓글이 없습니다.";
    private static final String BOARD_EXPIRATION = "게시판 활성 시간이 만료되었습니다.";
    private static final String BOARD_KEY_DELIMITER  = ":";
    private static final String NOT_COMMENT_WRITER = "댓글 작성자가 아닙니다.";
    private static final String NOT_MODIFIABLE_COMMENTS = "댓글이 수정/삭제 불가능한 상태입니다.";
    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";

    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final BoardRedisService boardRedisService;
    private final BoardRepository boardRepository;

    /**
     * 댓글 작성
     * - 회원만 가능, 컨트롤러에서 사용자 인증 객체 members를 받음
     * - 게시판 활성화인 경우에만 댓글 작성 가능
     */
    @Transactional
    public Comments saveComments(Members member, SaveCommentsDto saveCommentsDto) {
        // fetch join을 통한 게시글과 게시판 조회
        Posts posts = postsRepository.findByIdWithBoard(saveCommentsDto.getPostId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS)
                );

        // board 객체를 posts 그래프 탐색을 통해 획득 및 검증 후, boardName 초기화
        Boards boards = posts.getBoards();
        validateBoard(boards);
        saveCommentsDto.setBoardName(boards.getName());

        // 게시판 비활성화인 경우, 댓글 작성 불가능 예외
        if(!boardRedisService.isRealTimeBoard(saveCommentsDto)) {
            throw new BoardExpiredException(BOARD_EXPIRATION);
        }

        return commentsRepository.save(Comments.builder()
                .content(saveCommentsDto.getContent())
                .members(member)
                .posts(posts)
                .build());
    }

    /**
     * 그래프 탐색으로 획득한 게시판의 유효성 검사
     */
    private void validateBoard(Boards boards) {
        if(boards == null || boards.getId() == null || boards.getId().equals(0L)) {
            throw new NotFoundException(NOT_EXIST_BOARD);
        }
    }

    /**
     * 댓글 삭제 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 삭제 가능
     * - 게시판 활성화 동안에만 삭제 가능
     * - modifiable = true 조건에서 삭제 가능
     */
    @Transactional
    public void deleteCommentsByCommentId(Members member, DeleteCommentsDto deleteCommentsDto) {
        // 댓글 존재 확인
        Comments comments = commentsRepository.findById(deleteCommentsDto.getCommentId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

        // 본인이 작성한 댓글만 삭제가 가능
        if(!comments.isCommentsWriter(comments, member)) {
            throw new InvalidRequestException(NOT_COMMENT_WRITER);
        }

        // 게시판 활성화 동안에만 삭제 가능
        // board 객체를 posts 그래프 탐색을 통해 획득 및 검증 후, boardName 초기화
        Boards boards = comments.getPosts().getBoards();
        validateBoard(boards);
        deleteCommentsDto.setBoardName(boards.getName());

        if(!boardRedisService.isRealTimeBoard(deleteCommentsDto)) {
            throw new BoardExpiredException(BOARD_EXPIRATION);
        }

        // modifiable이 true인 경우에만 삭제 가능
        if (!comments.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_COMMENTS);
        }

        // 댓글 삭제 처리
        commentsRepository.deleteById(deleteCommentsDto.getCommentId());
    }

    /**
     * 댓글 수정 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 수정 가능
     * - 게시판 활성화 동안에만 삭제 가능
     * - modifiable = true 조건에서 삭제 가능
     */
    @Transactional
    public void updateCommentsByMembersAndCommentId(Members members, UpdateCommentsDto updateCommentsDto) {
        // 댓글 존재 확인
        Comments comments = commentsRepository.findById(updateCommentsDto.getCommentId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

        // 본인이 작성한 댓글만 수정이 가능
        if(!comments.isCommentsWriter(comments, members)) {
            throw new InvalidRequestException(NOT_COMMENT_WRITER);
        }

        // 게시판 활성화 동안에만 수정 가능
        // board 객체를 posts 그래프 탐색을 통해 획득 및 검증 후, boardName 초기화
        Boards boards = comments.getPosts().getBoards();
        validateBoard(boards);
        updateCommentsDto.setBoardName(boards.getName());

        if(!boardRedisService.isRealTimeBoard(updateCommentsDto)) {
            throw new BoardExpiredException(BOARD_EXPIRATION);
        }

        // modifiable이 true인 경우에만 삭제 가능
        if (!comments.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_COMMENTS);
        }

        // 댓글 수정 처리
        comments.update(updateCommentsDto);
    }

    public Page<CommentInfoDto> getCommentsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentsRepository.findByMemberIdWithPost(memberId, pageable);
    }

    /**
     * 게시글이 속한 게시판의 타이머가 만료됐을 경우 modifiable 필드의 값을 false로 변경합니다.
     * modifiable = false가 수정/삭제 불가능
     */
    @Transactional
    public void updateModifiable(Long boardId) {
        commentsRepository.updateFlagByBoardId(boardId);
    }

    /**
     * 댓글을 조회할 때, 페이지네이션에 따른 기본 댓글 정보와 총 댓글 개수와 페이지 개수를 같이 제공
     */
    public List<FindAllCommentsDto> findAllCommentsByPostId(Long postId, Pageable pageable) {
        // 전체 댓글 수 및 페이지 수 계산
        int totalCommentsCount = commentsRepository.countByPostsId(postId);
        int totalPageCount = (int) Math.ceil((double) totalCommentsCount / pageable.getPageSize());

        // totalCommentsCount = 0 이면, 댓글이 없으므로 바로 return
        if(totalCommentsCount == 0) {
            return Collections.emptyList();
        }

        // 댓글 데이터 조회 (기본 필드만)
        List<FindAllCommentsDto> comments = commentsRepository.findByPostsIdOrderByCreatedAtDesc(postId, pageable);

        // totalCommentsCount와 totalPageCount를 포함한 새로운 DTO 리스트 생성하여 반환
        return comments.stream()
                .map(comment -> FindAllCommentsDto.builder()
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .id(comment.getId())
                        .content(comment.getContent())
                        .modifiable(comment.isModifiable())
                        .writer(comment.getWriter())
                        .totalCommentsCount(totalCommentsCount)
                        .totalPageCount(totalPageCount)
                        .build())
                .toList();
    }
}
