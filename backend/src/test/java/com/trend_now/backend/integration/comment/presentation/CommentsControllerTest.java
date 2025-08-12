package com.trend_now.backend.integration.comment.presentation;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.vo.SaveCommentsRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentsControllerTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentsController commentsController;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Posts testPost;
    private Members testMembers;
    private Boards testBoards;

    @BeforeEach
    public void setUp() {
        testMembers = memberRepository.save(Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build());

        testBoards = boardRepository.save(Boards.builder()
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
    @DisplayName("회원만 댓글 작성이 가능하다")
    void 비회원_댓글_작성_불가능() {
        // given
        // 로그인 안한 상태로 댓글 작성을 요청
        SaveCommentsRequest saveCommentsRequest = new SaveCommentsRequest("testContent");

        // when & then
        // 유저 인증 객체 members에 대한 정보가 null 이므로 댓글 작성이 불가능해야 한다.
        assertThatThrownBy(() -> commentsController.saveComments(
                testBoards.getId(), testPost.getId(), null, saveCommentsRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("회원이 아닙니다.");
    }
}