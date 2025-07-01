package com.trend_now.backend.comment.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.application.CommentsService;
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

import java.util.ArrayList;
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

    @Autowired
    private CommentsService commentsService;

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
    @DisplayName("findAllCommentsByPostId가 페이지네이션과 함께 totalCommentsCount, totalPageCount를 포함하여 반환하는지 검증")
    public void 게시글_모든_댓글_조회_결과_확인() {
        // Given
        // 댓글 5개를 동일 게시글에 저장
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

        Comments comment5 = Comments.builder()
                .content("다섯 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        commentsRepository.save(comment1);
        commentsRepository.save(comment2);
        commentsRepository.save(comment3);
        commentsRepository.save(comment4);
        commentsRepository.save(comment5);

        em.flush();
        em.clear();

        // When
        // 5개 댓글을 페이지 당 2개로 하여, 3 페이지를 생성
        // 첫 번째 페이지 조회
        Pageable pageable1 = PageRequest.of(0, 2);
        List<FindAllCommentsDto> actualDtos1 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable1);

        // 두 번째 페이지 조회
        Pageable pageable2 = PageRequest.of(1, 2);
        List<FindAllCommentsDto> actualDtos2 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable2);

        // 세 번째 페이지 조회
        Pageable pageable3 = PageRequest.of(2, 2);
        List<FindAllCommentsDto> actualDtos3 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable3);

        // Then
        // 첫 번째 페이지 검증 (2개 댓글)
        assertThat(actualDtos1).hasSize(2);
        actualDtos1.forEach(dto -> {
            assertThat(dto.getTotalCommentsCount()).isEqualTo(5);
            assertThat(dto.getTotalPageCount()).isEqualTo(3);
            assertThat(dto.getContent()).isNotBlank();
            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getCreatedAt()).isNotNull();
        });

        // 두 번째 페이지 검증 (2개 댓글)
        assertThat(actualDtos2).hasSize(2);
        actualDtos2.forEach(dto -> {
            assertThat(dto.getTotalCommentsCount()).isEqualTo(5);
            assertThat(dto.getTotalPageCount()).isEqualTo(3);
            assertThat(dto.getContent()).isNotBlank();
            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getCreatedAt()).isNotNull();
        });

        // 세 번째 페이지 검증 (1개 댓글)
        assertThat(actualDtos3).hasSize(1);
        actualDtos3.forEach(dto -> {
            assertThat(dto.getTotalCommentsCount()).isEqualTo(5);
            assertThat(dto.getTotalPageCount()).isEqualTo(3);
            assertThat(dto.getContent()).isNotBlank();
            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getCreatedAt()).isNotNull();
        });

        // 생성일 기준 내림차순 정렬 확인
        List<FindAllCommentsDto> allDtos = new ArrayList<>();
        allDtos.addAll(actualDtos1);
        allDtos.addAll(actualDtos2);
        allDtos.addAll(actualDtos3);

        for (int i = 0; i < allDtos.size() - 1; i++) {
            assertThat(allDtos.get(i).getCreatedAt())
                    .isAfterOrEqualTo(allDtos.get(i + 1).getCreatedAt());
        }

        System.out.println("첫 번째 페이지 댓글 수: " + actualDtos1.size());
        System.out.println("두 번째 페이지 댓글 수: " + actualDtos2.size());
        System.out.println("세 번째 페이지 댓글 수: " + actualDtos3.size());
        System.out.println("전체 댓글 수: " + actualDtos1.get(0).getTotalCommentsCount());
        System.out.println("전체 페이지 수: " + actualDtos1.get(0).getTotalPageCount());
    }

    @Test
    @DisplayName("페이지 크기가 전체 댓글 수보다 클 때 totalPageCount가 1이 되는지 검증")
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
        List<FindAllCommentsDto> actualDtos = commentsService.findAllCommentsByPostId(testPost.getId(), pageable);

        // Then
        assertThat(actualDtos).hasSize(2);
        actualDtos.forEach(dto -> {
            assertThat(dto.getTotalCommentsCount()).isEqualTo(2);
            assertThat(dto.getTotalPageCount()).isEqualTo(1);
        });

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
        List<FindAllCommentsDto> actualDtos = commentsService.findAllCommentsByPostId(testPost.getId(), pageable);

        // Then
        assertThat(actualDtos).isEmpty();
    }

    @Test
    @DisplayName("댓글이 없는 게시글 조회 시 빈 리스트와 0개 카운트가 반환되는지 검증")
    public void 댓글이_없는_게시글_조회_테스트() {
        // Given
        // 댓글을 저장하지 않음

        // When
        Pageable pageable = PageRequest.of(0, 10);
        List<FindAllCommentsDto> actualDtos = commentsService.findAllCommentsByPostId(testPost.getId(), pageable);

        // Then
        assertThat(actualDtos).isEmpty();
    }

    @Test
    @DisplayName("totalPageCount 계산이 정확한지 검증")
    public void totalPageCount_계산_검증() {
        // Given
        // 3개의 댓글 저장
        for (int i = 1; i <= 3; i++) {
            Comments comment = Comments.builder()
                    .content(i + "번째 댓글입니다.")
                    .posts(testPost)
                    .members(testMembers)
                    .build();
            commentsRepository.save(comment);
        }

        em.flush();
        em.clear();

        // When & Then
        // 페이지 크기 2일 때, 3개 댓글은 2페이지로 계산
        Pageable pageable1 = PageRequest.of(0, 2);
        List<FindAllCommentsDto> result1 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable1);
        assertThat(result1.get(0).getTotalPageCount()).isEqualTo(2);

        // 페이지 크기 3일 때, 3개 댓글은 1페이지로 계산
        Pageable pageable2 = PageRequest.of(0, 3);
        List<FindAllCommentsDto> result2 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable2);
        assertThat(result2.get(0).getTotalPageCount()).isEqualTo(1);

        // 페이지 크기 1일 때, 3개 댓글은 3페이지로 계산
        Pageable pageable3 = PageRequest.of(0, 1);
        List<FindAllCommentsDto> result3 = commentsService.findAllCommentsByPostId(testPost.getId(), pageable3);
        assertThat(result3.get(0).getTotalPageCount()).isEqualTo(3);
    }
}