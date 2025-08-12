package com.trend_now.backend.integration.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.CommentInfoDto;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentsServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boaardRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Posts testPost;
    private Members testMembers;
    private Boards testBoards;

    private static final String BOARD_KEY_DELIMITER = ":";

    @BeforeEach
    public void setUp() {
        testMembers = memberRepository.save(Members.builder()
            .name("testUser")
            .email("testEmail")
            .snsId("testSnsId")
            .provider(Provider.TEST)
            .build());

        testBoards = boaardRepository.save(Boards.builder()
            .name("testBoards")
            .boardCategory(BoardCategory.REALTIME)
            .build());

        testPost = postsRepository.save(Posts.builder()
            .title("testTitle")
            .writer("testWriter")
            .content("testContent")
            .boards(testBoards)
            .members(testMembers)
            .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("자신이 작성한 댓글만 수정 또는 삭제를 할 수 있다.")
    void 댓글_작성자만_수정_또는_삭제_가능() {
        // given
        // redis에 게시판 저장 (BOARD_TTL이 존재하는 상태로 만들기)
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testBoards.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        // testMembers가 comments1 댓글 작성
        SaveCommentsDto testSaveCommentsDto =
            SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(),
                "testContent");

        commentsService.saveComments(testMembers, testSaveCommentsDto);

        // 다른 사용자 생성 (testMembers2)
        Members testMembers2 = memberRepository.save(Members.builder()
            .name("testUser2")
            .email("testEmail2")
            .snsId("testSnsId2")
            .provider(Provider.TEST)
            .build());

        em.flush();
        em.clear();

        // 작성된 댓글 조회
        List<Comments> commentsList = commentsRepository.findAll();
        Comments comments1 = commentsList.get(0);

        // 삭제 요청 DTO 생성
        DeleteCommentsDto deleteCommentsDto = DeleteCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId());

        // 수정 요청 DTO 생성
        UpdateCommentsDto updateCommentsDto = UpdateCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId(),
            "updated content");

        // when & then
        // 임의 testMembers2가 comments1 댓글을 삭제 처리 요청 시,
        // InvalidRequestException 예외에 NOT_COMMENT_WRITER 메시지 발생해야 한다.
        assertThatThrownBy(
            () -> commentsService.deleteCommentsByCommentId(testMembers2, deleteCommentsDto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("댓글 작성자가 아닙니다.");

        // 임의 testMembers2가 comments1 댓글을 수정 처리 요청 시,
        // InvalidRequestException 예외에 NOT_COMMENT_WRITER 메시지 발생해야 한다.
        assertThatThrownBy(() -> commentsService.updateCommentsByMembersAndCommentId(testMembers2,
            updateCommentsDto))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("댓글 작성자가 아닙니다.");

        // 댓글이 여전히 존재하고 변경되지 않았는지 확인
        commentsList = commentsRepository.findAll();
        assertThat(commentsList).hasSize(1);
        assertThat(commentsList.get(0).getContent()).isEqualTo("testContent");
        assertThat(commentsList.get(0).getMembers().getId()).isEqualTo(testMembers.getId());
    }

    @Test
    @DisplayName("내가 작성한 댓글을 조회할 수 있다.")
    public void 내가_작성한_댓글_조회() throws Exception {
        //given
        // 게시판 활성화
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testBoards.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        for (int i = 1; i <= 10; i++) {
            SaveCommentsDto testSaveCommentsDto = SaveCommentsDto.of(testBoards.getId(),
                testPost.getId(), testBoards.getName(), "testContent" + i);
            commentsService.saveComments(testMembers, testSaveCommentsDto);
        }

        //when
        Page<CommentInfoDto> commentsByMemberId = commentsService.getCommentsByMemberId(
            testMembers.getId(), 0, 5);

        //then
        assertThat(commentsByMemberId.getTotalPages()).isEqualTo(2);
        // 최신순으로 조회되기 때문에 마지막에 저장된 댓글이 첫번째 값
        assertThat(commentsByMemberId.getContent().getFirst().getContent()).isEqualTo("testContent10");
        assertThat(commentsByMemberId.getContent().getLast().getPostTitle()).isEqualTo(testPost.getTitle());

    }

    @Test
    @DisplayName("게시판이 재등록 시 기존에 작성된 댓글은 수정, 삭제가 불가능하다")
    public void 게시판_재등록_시_기존에_작성된_게시글은_수정_삭제가_불가능하다() {
        //given
        // 댓글 작성 후, 게시판 활성화 시간이 끝나 삭제되고 추후에 재생성된다.

        // 게시판 활성화
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testBoards.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        // 댓글 작성
        SaveCommentsDto testSaveCommentsDto = SaveCommentsDto.of(testBoards.getId(),
                testPost.getId(), testBoards.getName(), "testContent");
        Comments testComments = commentsService.saveComments(testMembers, testSaveCommentsDto);

        // 게시판 활성화 시간 끝과 동시에 작성된 댓글은 수정/삭제 비활성화
        redisTemplate.delete(key);
        BoardSaveDto boardSaveDto = new BoardSaveDto(
                testBoards.getId(), testBoards.getName(), testBoards.getBoardCategory());
        boardService.updateBoardIsDeleted(boardSaveDto, false);
        commentsService.updateModifiable(testBoards.getId()); // SignalKeywordJobListener에서 하는 일 직접 실행
        em.flush();
        em.clear();

        // 게시판 재생성
        boardService.updateBoardIsDeleted(boardSaveDto, true);
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        //when & then
        // 게시판 재생성 되기 전의 기존의 댓글은 수정과 삭제가 불가능해야 한다.
        DeleteCommentsDto deleteCommentsDto = DeleteCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), testComments.getId());

        UpdateCommentsDto updateCommentsDto = UpdateCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), testComments.getId(),
                "updated content");

        assertThatThrownBy(
                () -> commentsService.deleteCommentsByCommentId(testMembers, deleteCommentsDto))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("댓글이 수정/삭제 불가능한 상태입니다.");

        assertThatThrownBy(() -> commentsService.updateCommentsByMembersAndCommentId(testMembers,
                updateCommentsDto))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("댓글이 수정/삭제 불가능한 상태입니다.");
    }
}