package com.trend_now.backend.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.CommentListPagingResponseDto;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardTtlException;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
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
    @DisplayName("BOARD_TTL 존재 여부에 따라 boardTtlStatus가 다르게 저장된다")
    void BOARD_TTL_여부에_따라_boardTtlStatus가_결정() {
        // given
        // redis에 게시판 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
            SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(),
                "testContent");

        // when
        // BOARD_TTL 존재 했을 때, 댓글을 작성
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        // BOARD_TTL 존재하지 않았을 때, 댓글을 작성
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        // then
        List<Comments> commentsList = commentsRepository.findAll();

        assertThat(commentsList).hasSize(2);

        Comments beforeTtlComments = commentsList.get(0);
        Comments afterTtlComments = commentsList.get(1);

        System.out.println("beforeTtlComments : " + beforeTtlComments.toString());
        System.out.println("afterTtlComments : " + afterTtlComments.toString());

        // beforeTtlComment의 BoardTtlStatus는 "BOARD_TTL_BEFORE" 값을 가짐
        assertThat(beforeTtlComments.getBoardTtlStatus()).isEqualTo(
            BoardTtlStatus.BOARD_TTL_BEFORE);

        // afterTtlComment의 BoardTtlStatus는 "BOARD_TTL_AFTER" 값을 가짐
        assertThat(afterTtlComments.getBoardTtlStatus()).isEqualTo(BoardTtlStatus.BOARD_TTL_AFTER);

    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 댓글 삭제가 결정된다")
    void BOARD_TTL_여부에_따라_댓글_삭제_결정() {
        // given
        // redis에 게시판 저장 및 댓글 2개 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
            SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(),
                "testContent");

        commentsService.saveComments(testMembers, testSaveCommentsDto);
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        List<Comments> commentsList = commentsRepository.findAll();
        Comments comments1 = commentsList.get(0);
        Comments comments2 = commentsList.get(1);

        DeleteCommentsDto deleteCommentsDto1 = DeleteCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId());

        DeleteCommentsDto deleteCommentsDto2 = DeleteCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments2.getId());

        // when & then
        // 하나는 BOARD_TTL 전에 삭제하여 삭제됨을 확인 & 나머지 하나는 BOARD_TTL 후에 삭제하지만 삭제안됨을 확인
        commentsService.deleteCommentsByCommentId(testMembers, deleteCommentsDto1);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        assertThatThrownBy(
            () -> commentsService.deleteCommentsByCommentId(testMembers, deleteCommentsDto2))
            .isInstanceOf(BoardTtlException.class)
            .hasMessage("게시판 활성 시간이 만료되었습니다.");

        commentsList = commentsRepository.findAll();
        System.out.println("commentsList >>> " + commentsList.toString());
        assertThat(commentsList.size()).isEqualTo(1);
        assertThat(commentsList.get(0)).isEqualTo(comments2);
    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 댓글 수정이 결정된다")
    void BOARD_TTL_여부에_따라_댓글_수정_결정() {
        // given
        // redis에 게시판 저장 및 댓글 2개 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
            SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(),
                "testContent");

        commentsService.saveComments(testMembers, testSaveCommentsDto);
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        List<Comments> commentsList = commentsRepository.findAll();
        Comments comments1 = commentsList.get(0);
        Comments comments2 = commentsList.get(1);

        UpdateCommentsDto updateCommentsDto1 = UpdateCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId(),
            "test updated comments1");

        UpdateCommentsDto updateCommentsDto2 = UpdateCommentsDto.of(
            testBoards.getId(), testPost.getId(), testBoards.getName(), comments2.getId(),
            "test updated comments1");

        // when & then
        // 하나는 BOARD_TTL 전에 수정하여 수정됨을 확인 & 나머지 하나는 BOARD_TTL 후에 수정하지만 수정안됨을 확인
        commentsService.updateCommentsByMembersAndCommentId(testMembers, updateCommentsDto1);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        assertThatThrownBy(() -> commentsService.updateCommentsByMembersAndCommentId(testMembers,
            updateCommentsDto2))
            .isInstanceOf(BoardTtlException.class)
            .hasMessage("게시판 활성 시간이 만료되었습니다.");

        commentsList = commentsRepository.findAll();
        System.out.println("commentsList >>> " + commentsList.toString());
        assertThat(commentsList.size()).isEqualTo(2);

        // 댓글1은 수정이 되고, 댓글2는 수정이 안됨을 확인
        assertThat(commentsList.get(0).getContent()).isEqualTo("test updated comments1");
        assertThat(commentsList.get(1).getContent()).isEqualTo("testContent");

    }

    @Test
    @DisplayName("자신이 작성한 댓글만 수정 또는 삭제를 할 수 있다.")
    void 댓글_작성자만_수정_또는_삭제_가능() {
        // given
        // redis에 게시판 저장 (BOARD_TTL이 존재하는 상태로 만들기)
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
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
        for (int i = 1; i <= 10; i++) {
            SaveCommentsDto testSaveCommentsDto = SaveCommentsDto.of(testBoards.getId(),
                testPost.getId(), testBoards.getName(), "testContent" + i);
            commentsService.saveComments(testMembers, testSaveCommentsDto);
        }
        //when
        CommentListPagingResponseDto commentsByMemberId = commentsService.getCommentsByMemberId(
            testMembers.getId(), 0, 5);

        //then
        assertThat(commentsByMemberId.getTotalPageCount()).isEqualTo(2);
        // 최신순으로 조회되기 때문에 마지막에 저장된 댓글이 첫번째 값
        assertThat(commentsByMemberId.getCommentsInfoListDto().getFirst().getContent()).isEqualTo("testContent10");
        assertThat(commentsByMemberId.getCommentsInfoListDto().getLast().getPostTitle()).isEqualTo(testPost.getTitle());

    }
}