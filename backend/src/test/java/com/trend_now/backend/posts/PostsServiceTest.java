package com.trend_now.backend.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.user.domain.Users;
import com.trend_now.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class PostsServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostsService postsService;

    private Users users;
    private Boards boards;

    @BeforeEach
    public void before() {
        users = Users.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider("testProvider")
                .build();
        userRepository.save(users);

        boards = Boards.builder()
                .name("testBoard")
                .boardCategory(BoardCategory.REALTIME)
                .build();
        boardRepository.save(boards);

        for (int i = 1; i <= 10; i++) {
            PostsSaveDto postsSaveDto = PostsSaveDto.of(boards.getId(), "title" + i, "content" + i);
            postsService.savePosts(postsSaveDto, users);
        }
    }

    @Test
    @DisplayName("회원은 게시글을 작성할 수 있다.")
    public void 게시글_작성() throws Exception {
        //given
        PostsSaveDto postsSaveDto = PostsSaveDto.of(boards.getId(), "testTitle", "testContent");

        //when
        Long savePosts = postsService.savePosts(postsSaveDto, users);

        //then
        PostsInfoDto postsInfoDto = postsService.findPostsById(savePosts);
        assertThat(savePosts).isNotNull().isGreaterThan(0);
        assertThat(postsInfoDto.getTitle()).isEqualTo(postsSaveDto.getTitle());
        assertThat(postsInfoDto.getContent()).isEqualTo(postsSaveDto.getContent());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5",  // 첫 번째 페이지, 5개 게시글
            "1, 5",  // 두 번째 페이지, 5개 게시글
            "0, 10", // 첫 번째 페이지, 10개 게시글 (모두 가져오기)
            "2, 5"   // 세 번째 페이지, 게시글 없음 (경계 테스트)
    })
    @DisplayName("게시판별 게시글 페이징 조회")
    public void 게시글_페이징_조회(int page, int size) {
        //given
        PostsPagingRequestDto requestDto = new PostsPagingRequestDto(boards.getId(), page, size);

        //when
        Page<PostsInfoDto> result = postsService.findAllPostsPagingByBoardId(requestDto);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(size);

        if (page == 2) {
            assertThat(result.getContent()).isEmpty();
        } else {
            assertThat(result.getContent()).isNotEmpty();
        }
    }
}
