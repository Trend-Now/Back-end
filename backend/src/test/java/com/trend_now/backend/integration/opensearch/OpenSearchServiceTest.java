package com.trend_now.backend.integration.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.opensearch.service.OpenSearchService;
import com.trend_now.backend.search.dto.BoardRedisKey;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class OpenSearchServiceTest {

    @Autowired
    private OpenSearchService openSearchService;

//    @BeforeEach
//    void setUp() {
//        try {
//            // 테스트 시작 전 인덱스 삭제 (깨끗한 상태 유지)
//            boolean exists = openSearchClient.indices().exists(e -> e.index("realtime_keyword")).value();
//            if (exists) {
//                openSearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index("realtime_keyword")));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        // 인덱스 생성 (nori 분석기 적용 확인)
//        openSearchService.initIndex();
//    }

    @Test
    @DisplayName("한글 형태소 분석기를 통한 유사 키워드 검색 테스트")
    void findSimilarKeywordTest() throws InterruptedException {
        // given
        String originalKeyword = "쿠팡 정보 유출 사태 사과";
        Long boardId = 1L;
        BoardRedisKey keyProvider = new BoardRedisKey(boardId, originalKeyword);

        String[] testKeywords = originalKeyword.split(" ");
        // when
        openSearchService.saveKeyword(keyProvider);

        // 인덱싱 반영을 위해 잠시 대기 (OpenSearch refresh_interval 기본값 1초)
        Thread.sleep(2000);

        // then
        // 1. 완전 일치 검색
        System.out.println("1. 완전 일치 검색 테스트: " + originalKeyword);
        BoardRedisKey result1 = openSearchService.findSimilarKeyword(originalKeyword);
        assertThat(result1).isNotNull();
        assertThat(result1.getBoardName()).isEqualTo(originalKeyword);

        // 2. 부분 키워드 검색 ("서민규"만 입력해도 찾아져야 함)
        System.out.println("2. 부분 키워드 검색 테스트: " + testKeywords[0]);
        BoardRedisKey result2 = openSearchService.findSimilarKeyword(testKeywords[0]);
        assertThat(result2).isNotNull();
        assertThat(result2.getBoardName()).isEqualTo(originalKeyword);

        // 3. 다른 부분 키워드 검색 ("경기"만 입력해도 찾아져야 함)
        System.out.println("3. 부분 키워드 검색 테스트: " + testKeywords[2]);
        BoardRedisKey result3 = openSearchService.findSimilarKeyword(testKeywords[2]);
        assertThat(result3).isNotNull();
        assertThat(result3.getBoardName()).isEqualTo(originalKeyword);
        
        // 4. 엉뚱한 키워드 검색 (결과 없어야 함)
        System.out.println("4. 불일치 검색 테스트: 엉뚱한검색어");
        BoardRedisKey result4 = openSearchService.findSimilarKeyword("엉뚱한검색어");
        assertThat(result4).isNull();
    }
}