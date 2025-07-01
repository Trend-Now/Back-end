package com.trend_now.backend.comment.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentsRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentsRepository commentsRepository;

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
    @DisplayName("findByPostsIdOrderByCreatedAtDesc가 페이지네이션과 함께 FindAllCommentsDto로 프로젝션되는지 검증")
    public void 게시글_모든_댓글_조회_결과_확인() {
        // Given
        // 댓글 4개를 동일 게시글에 저장 (페이지네이션 테스트를 위해 더 많은 데이터)
        Comments comment1 = Comments.builder()
                .content("첫 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        Comments comment2 = Comments.builder()
                .content("두 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        Comments comment3 = Comments.builder()
                .content("세 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        Comments comment4 = Comments.builder()
                .content("네 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        commentsRepository.save(comment1);
        commentsRepository.save(comment2);
        commentsRepository.save(comment3);
        commentsRepository.save(comment4);

        em.flush();
        em.clear();

        // When
        // 첫 번째 페이지 조회 (페이지 크기: 2)
        Pageable pageable1 = PageRequest.of(0, 2);
        List<FindAllCommentsDto> actualDtos1 = commentsRepository.findByPostsIdOrderByCreatedAtDesc(testPost.getId(), pageable1);

        // 두 번째 페이지 조회 (페이지 크기: 2)
        Pageable pageable2 = PageRequest.of(1, 2);
        List<FindAllCommentsDto> actualDtos2 = commentsRepository.findByPostsIdOrderByCreatedAtDesc(testPost.getId(), pageable2);

        // findAll()로 조회한 후 수동으로 변환
        List<Comments> allComments = commentsRepository.findAll().stream()
                .filter(comment -> comment.getPosts().getId().equals(testPost.getId()))
                .collect(Collectors.toList());

        // 생성일 기준 내림차순 정렬
        allComments.sort(Comparator.comparing(Comments::getCreatedAt).reversed());

        // 첫 번째 페이지 예상 결과 (0번째, 1번째 댓글)
        List<FindAllCommentsDto> expectedDtos1 = allComments.stream()
                .limit(2)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 두 번째 페이지 예상 결과 (2번째, 3번째 댓글)
        List<FindAllCommentsDto> expectedDtos2 = allComments.stream()
                .skip(2)
                .limit(2)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // Then
        // 첫 번째 페이지 검증
        assertThat(actualDtos1).hasSize(2);
        assertThat(expectedDtos1).hasSize(2);

        for (int i = 0; i < actualDtos1.size(); i++) {
            FindAllCommentsDto actual = actualDtos1.get(i);
            FindAllCommentsDto expected = expectedDtos1.get(i);

            System.out.println("첫 번째 페이지 " + i + "번째 actualDtos > " + actual.toString());
            System.out.println("첫 번째 페이지 " + i + "번째 expectedDtos > " + expected.toString());

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getContent()).isEqualTo(expected.getContent());
            assertThat(actual.getCreatedAt()).isEqualToIgnoringNanos(expected.getCreatedAt());
        }

        // 두 번째 페이지 검증
        assertThat(actualDtos2).hasSize(2);
        assertThat(expectedDtos2).hasSize(2);

        for (int i = 0; i < actualDtos2.size(); i++) {
            FindAllCommentsDto actual = actualDtos2.get(i);
            FindAllCommentsDto expected = expectedDtos2.get(i);

            System.out.println("두 번째 페이지 " + i + "번째 actualDtos > " + actual.toString());
            System.out.println("두 번째 페이지 " + i + "번째 expectedDtos > " + expected.toString());

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getContent()).isEqualTo(expected.getContent());
            assertThat(actual.getCreatedAt()).isEqualToIgnoringNanos(expected.getCreatedAt());
        }

        System.out.println("첫 번째 페이지 actualDtos > " + actualDtos1.toString());
        System.out.println("두 번째 페이지 actualDtos > " + actualDtos2.toString());
        System.out.println("전체 allComments > " + allComments.toString());
    }

    @Test
    @DisplayName("페이지 크기가 전체 댓글 수보다 클 때 모든 댓글이 조회되는지 검증")
    public void 페이지_크기가_큰_경우_테스트() {
        // Given
        Comments comment1 = Comments.builder()
                .content("첫 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        Comments comment2 = Comments.builder()
                .content("두 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        commentsRepository.save(comment1);
        commentsRepository.save(comment2);

        em.flush();
        em.clear();

        // When
        // 페이지 크기를 전체 댓글 수보다 크게 설정
        Pageable pageable = PageRequest.of(0, 10);
        List<FindAllCommentsDto> actualDtos = commentsRepository.findByPostsIdOrderByCreatedAtDesc(testPost.getId(), pageable);

        // Then
        assertThat(actualDtos).hasSize(2);

        // 생성일 기준 내림차순으로 정렬되었는지 확인
        assertThat(actualDtos.get(0).getCreatedAt()).isAfter(actualDtos.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("존재하지 않는 페이지 조회 시 빈 리스트가 반환되는지 검증")
    public void 존재하지_않는_페이지_조회_테스트() {
        // Given
        Comments comment1 = Comments.builder()
                .content("첫 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        commentsRepository.save(comment1);

        em.flush();
        em.clear();

        // When
        // 존재하지 않는 페이지 조회 (전체 댓글 1개인데 2번째 페이지 조회)
        Pageable pageable = PageRequest.of(1, 10);
        List<FindAllCommentsDto> actualDtos = commentsRepository.findByPostsIdOrderByCreatedAtDesc(testPost.getId(), pageable);

        // Then
        assertThat(actualDtos).isEmpty();
    }
    private FindAllCommentsDto convertToDto(Comments comment) {
        return FindAllCommentsDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}