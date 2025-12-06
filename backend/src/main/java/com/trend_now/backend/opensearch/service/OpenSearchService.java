package com.trend_now.backend.opensearch.service;

import com.trend_now.backend.board.dto.BoardKeyProvider;
import com.trend_now.backend.search.dto.BoardRedisKey;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchService {

    private static final String INDEX_NAME = "realtime_keyword";
    private static final String KEYWORD_FIELD = "boardName";
    private static final String SEARCH_SIMILARITY_THRESHOLD = "30%";
    private static final String OPENSEARCH_CONNECTION_ERROR = "OpenSearch 연결에 실패했습니다.";
    private static final String OPENSEARCH_CONNECTION_OR_NOT_FOUND_ERROR =
        "OpenSearch에서" + INDEX_NAME + " 인덱스를 찾을 수 없거나, " + OPENSEARCH_CONNECTION_ERROR;

    private final OpenSearchClient openSearchClient;

    public void initIndex() {
        try {
            boolean exists = openSearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            // realtime_keyword 인덱스가 존재하지 않는 경우에 인덱스 생성
            if (!exists) {
                // TODO: DB에 저장된 기존 키워드들을 모두 불러와서 OpenSearch에 색인 생성

                openSearchClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m
                        .properties(KEYWORD_FIELD, p -> p.text(k -> k.analyzer("nori")))
                    )
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(OPENSEARCH_CONNECTION_ERROR);
        }
    }

    public void saveKeyword(BoardKeyProvider boardKeyProvider) {
        try {
            openSearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(boardKeyProvider.getBoardId()))
                .document(new BoardRedisKey(boardKeyProvider.getBoardId(), boardKeyProvider.getBoardName()))
            );
        } catch (IOException e) {
            throw new RuntimeException(OPENSEARCH_CONNECTION_OR_NOT_FOUND_ERROR);
        }
    }

    public BoardRedisKey findSimilarKeyword(String newKeyword) {
        try {
            SearchResponse<BoardRedisKey> searchResult = openSearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                        .match(m -> m
                            .field(KEYWORD_FIELD)
                            .query(v -> v.stringValue(newKeyword))
                            // 유사도 임계값 설정
                            .minimumShouldMatch(SEARCH_SIMILARITY_THRESHOLD)
                        )
                    )
                    .size(10),
                BoardRedisKey.class
            );

            List<Hit<BoardRedisKey>> hits = searchResult.hits().hits();
            if (hits == null || hits.isEmpty()) {
                return null;
            }
            return hits.getFirst().source();
        } catch (IOException e) {
            throw new RuntimeException(OPENSEARCH_CONNECTION_OR_NOT_FOUND_ERROR);
        }
    }
}
