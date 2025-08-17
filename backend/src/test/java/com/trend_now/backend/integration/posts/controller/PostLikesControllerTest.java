package com.trend_now.backend.integration.posts.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trend_now.backend.integration.annotation.WithMockCustomUser;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class PostLikesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Boards boards;
    private Posts posts;
    private Members members;

    @BeforeEach
    public void beforeEach() {
        boards = Boards.builder()
                .name("testBoard")
                .boardCategory(BoardCategory.REALTIME)
                .build();
        boardRepository.save(boards);

        posts = Posts.builder()
                .title("testTitle")
                .writer("testWriter")
                .content("testContent")
                .boards(boards)
                .build();
        postsRepository.save(posts);

        members = Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build();
        memberRepository.save(members);
    }

    @WithMockCustomUser
    @Test
    @DisplayName("회원이 좋아요 버튼을 눌렀을 때 좋아요의 개수가 증가한다")
    public void 좋아요_증가() throws Exception {
        //given
        Long postId = posts.getId();
        Long boardId = boards.getId();
        String boardName = boards.getName();

        //when
        //then
        mockMvc.perform(
                        post("/api/v1/boards/" + boardName + "/" + boardId + "/posts/" + postId + "/likes"))
                .andExpect(status().isOk());
    }

}
