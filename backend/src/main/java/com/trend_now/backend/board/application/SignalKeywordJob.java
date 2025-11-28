package com.trend_now.backend.board.application;

import com.trend_now.backend.board.application.board_summary.BoardSummaryTriggerService;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.*;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.opensearch.dto.OpenSearchDocumentDto;
import com.trend_now.backend.opensearch.service.OpenSearchService;
import com.trend_now.backend.search.dto.BoardRedisKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

@Slf4j
public class SignalKeywordJob implements Job {

    private static final String KEYWORD_JOB_ERROR_MESSAGE = "실시간 검색어 순위 리스트 스케줄러가 정상적으로 동작하지 않았습니다.";
    private static final String SIGNAL_KEYWORD_LIST_EVENT_MESSAGE = "실시간 검색어 순위 이벤트 호출";

    /**
     * 스케줄러에서 사용하는 모든 서비스를 담는 Record
     */
    private record Services(
            SignalKeywordService signalKeywordService,
            BoardRepository boardRepository,
            BoardService boardService,
            BoardRedisService boardRedisService,
            RedisPublisher redisPublisher,
            BoardCache boardCache,
            BoardSummaryTriggerService boardSummaryTriggerService,
            OpenSearchService openSearchService
    ) {}

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("실시간 검색어 순위 리스트 스케줄러 실행 시작 -  현재 시각: {}", Instant.now());

        ApplicationContext applicationContext = (ApplicationContext) context
                .getMergedJobDataMap().get("applicationContext");
        Services services = initializeServices(applicationContext);

        // 스케줄러 실행 동안 생기는 시간 차이를 없애기 위해 Instant.now()를 최초 1회만 호출
        Instant now = Instant.now();
        try {
            SignalKeywordDto signalKeywordDto = getSignalKeywordDto(services);

            for (Top10 top10 : signalKeywordDto.getTop10()) {
                /**
                 *  processKeyword는 크게 4가지 경우로 나뉘어 동작한다.
                 *  1. 실시간 게시판에 이미 똑같은 키워드가 존재하는 경우 -> Redis에 키워드 저장 및 순위 갱신
                 *  2. 실시간 게시판에 없지만 DB에는 저장 돼 있는 경우 -> DB isDeleted 상태 변경, Redis에 키워드 저장 (복원)
                 *  3. 똑같은 키워드가 실시간 게시판에 없지만, DB 혹은 Redis에 유사한 게시판은 존재하는 경우 -> DB 게시판 이름 업데이트, Redis에 키워드 저장 및 순위 갱신
                 *  4. 완전히 새로운 게시판인 경우 -> DB에 게시판 생성하고 저장, Redis에 키워드 저장
                 */
                processKeyword(top10, services, now);
            }

            // Redis(realtime_keywords)에 저장된 이전 실시간 검색어 순위 리스트 삭제
            services.signalKeywordService().deleteOldRealtimeKeywords();
            // Redis(board_rank_valid)의 유효시간 갱신
            services.boardRedisService().setRankValidListTime();
            services.signalKeywordService().updateLastUpdatedTime(signalKeywordDto.getNow());
            // 실시간 이슈 키워드 변경 SSE 이벤트 발행
            publishRealtimeKeywordsEvent(services);
        } catch (Exception e) {
            throw new JobExecutionException(KEYWORD_JOB_ERROR_MESSAGE, e);
        }
    }

    /**
     * 시그널 비즈 API를 호출하여 실시간 검색어 키워드 리스트를 조회
     */
    private static SignalKeywordDto getSignalKeywordDto(Services services) throws Exception {
        SignalKeywordDto signalKeywordDto = services.signalKeywordService()
                .fetchRealTimeKeyword().block();
        services.boardRedisService().cleanUpExpiredKeys();

        if (signalKeywordDto == null) {
            log.warn("시그널 비즈 API 호출 실패");
            throw new Exception(KEYWORD_JOB_ERROR_MESSAGE);
        }
        return signalKeywordDto;
    }

    /**
     * ApplicationContext에서 필요한 모든 Bean을 조회하여 Services Record로 반환
     */
    private Services initializeServices(ApplicationContext applicationContext) {
        return new Services(
                applicationContext.getBean(SignalKeywordService.class),
                applicationContext.getBean(BoardRepository.class),
                applicationContext.getBean(BoardService.class),
                applicationContext.getBean(BoardRedisService.class),
                applicationContext.getBean(RedisPublisher.class),
                applicationContext.getBean(BoardCache.class),
                applicationContext.getBean(BoardSummaryTriggerService.class),
                applicationContext.getBean(OpenSearchService.class)
        );
    }

    private double calculateScore(Instant now, int index) {
        // score 값을 현재 Instant 시간에서 2시간이 지난 값으로 설정하여 saveBoardRedis에 전달
        Instant twoHoursLater = now.plus(2, ChronoUnit.HOURS);

        long epochMilli = twoHoursLater.toEpochMilli(); // UTC 기준 2시간이 지난 시간을 밀리초로 변환
        double rank = (10 - index) * 0.01;

        return (epochMilli + rank) * -1; // 내림차순 정렬을 위해 음수로 변환
    }

    /**
     * 실시간 검색어 키워드 처리 메인 로직
     */
    private void processKeyword(Top10 top10, Services services, Instant now) {
        // signal.bz의 top10 객체를 BoardSaveDto 객체로 변환
        BoardSaveDto boardSaveDto = BoardSaveDto.from(top10);

        BoardRedisKey openSearchRedisKey = services.openSearchService.findSimilarKeyword(
                boardSaveDto.getBoardName());
        double score = calculateScore(now, top10.getRank());

        // DB에 비슷하거나 같은 게시판이 존재하는 경우
        if (openSearchRedisKey != null) {
            boolean isRealTimeBoard = services.boardRedisService.isRealTimeBoard(openSearchRedisKey);
            boardSaveDto.setBoardId(openSearchRedisKey.getBoardId());
            // OpenSearch에서 찾은 게시판이 실시간 게시판에 존재하는 경우
            if (isRealTimeBoard) {
                updateExistingRealtimeBoard(openSearchRedisKey, boardSaveDto, top10.getRank(), score, services);
                services.boardService.updateBoardName(openSearchRedisKey.getBoardId(), boardSaveDto.getBoardName());
            }
            // OpenSearch에서 찾은 게시판이 실시간 게시판에 존재하지 않는 경우
            restoreDeletedBoard(openSearchRedisKey, top10.getRank(), score, services);
            services.boardService.updateBoardName(openSearchRedisKey.getBoardId(), boardSaveDto.getBoardName());
            return;
        }

        // DB에 비슷하거나 같은 게시판이 없다면 완전히 새로운 게시판 생성
        handleNewBoard(boardSaveDto, top10.getRank(), score, services);
    }

    /**
     * 케이스 2, 케이스 3
     */
//    private void handleExistingBoard(BoardRedisKey openSearchRedisKey, int rank,
//                                     double score, Services services) {
//
//        boolean isRealTimeBoard = services.boardRedisService().isRealTimeBoard(openSearchRedisKey);
//
//        if (!isRealTimeBoard) {
//            // 케이스 2: 실시간 게시판에 없음 (복원)
//        } else {
//            // 케이스 3: 실시간 게시판에 이미 존재 (순위 갱신)
//            updateExistingRealtimeBoard(openSearchRedisKey,  rank, services);
//        }
//        services.boardCache().addRealtimeBoardCache(openSearchRedisKey);
//        services.boardRedisService().saveBoardRedis(openSearchRedisKey, score);
//    }

    /**
     * 케이스 1: 삭제 되었다가 다시 생성된 게시판 복원
     */
    private void restoreDeletedBoard(BoardRedisKey boardRedisKey, int rank, double score,
                                     Services services) {

        log.info("삭제 되었다가 다시 생성된 게시판 복원 처리 - boardId: {}, boardName: {}",
                boardRedisKey.getBoardId(), boardRedisKey.getBoardName());

        // Quartz Job에서 조회된 엔티티는 Detached 상태이므로 트랜잭션 내에서 조회 후 수정해야 함
        services.boardService().updateIsDeleted(boardRedisKey.getBoardId(), false);
        // 삭제 되었다가 다시 생성된 게시판이므로 게시판 요약 재생성
        services.boardSummaryTriggerService().triggerSummaryUpdate(boardRedisKey);
        // realtime_keywords에 NEW로 저장
        services.signalKeywordService().addNewRealtimeKeyword(boardRedisKey, rank);
        services.boardCache().addRealtimeBoardCache(boardRedisKey);
        services.boardRedisService().saveBoardRedis(boardRedisKey, score);
    }

    /**
     * 케이스 1: 기존 실시간 게시판 키워드 순위 갱신
     */
    private void updateExistingRealtimeBoard(BoardKeyProvider oldBoardKey, BoardSaveDto newBoardKey, int rank, double score,
                                             Services services) {

        log.info("기존 실시간 게시판 키워드 순위 갱신 - boardId: {}, boardName: {}",
                oldBoardKey.getBoardId(), oldBoardKey.getBoardName());

        services.signalKeywordService().addRealtimeKeywordWithRankTracking(oldBoardKey.getBoardId(),
                oldBoardKey.getBoardName(), newBoardKey.getBoardName(), rank);
        services.boardCache().addRealtimeBoardCache(newBoardKey);
        services.boardRedisService().saveBoardRedis(newBoardKey, score);
    }

    /**
     * 케이스 3: 유사한 게시판이 존재하는 경우 (게시판 이름 업데이트)
     */
//    private void handleSimilarBoard(BoardSaveDto boardSaveDto,
//                                    String oldBoardName, int rank, double score, Services services) {
//
//        log.info("Redis에서 유사한 게시판 조회 후 업데이트 - 기존 게시판명: {}, 새로운 게시판명: {}",
//                oldBoardName, boardSaveDto.getBoardName());
//
//        // 새로운 키워드 랭크 변동 추이 계산 후 realtime_keywords에 저장
//        services.signalKeywordService().addRealtimeKeywordWithRankTracking(boardSaveDto.getBoardId(), oldBoardName,
//                boardSaveDto.getBoardName(), rank);
//        // board_rank에 새로운 키워드 저장
//        services.boardRedisService().saveBoardRedis(boardSaveDto, score);
//        // 기존 게시판은 board_rank에서 삭제
//        services.boardRedisService().deleteKeyInBoardRank(boardSaveDto.getBoardId(), oldBoardName);
//        // 게시판 이름 변경
//        services.boardService().updateBoardName(boardSaveDto);
//        // 캐시 업데이트
//        services.boardCache().addRealtimeBoardCache(boardSaveDto);
//    }

    /**
     * 케이스 4: 완전히 새로운 게시판 생성
     */
    private void handleNewBoard(BoardSaveDto boardSaveDto, int rank, double score,
                                Services services) {

        log.info("완전히 새로운 게시판 생성 - boardName: {}", boardSaveDto.getBoardName());

        // 게시판 생성 후 저장
        Boards board = services.boardService().saveBoard(boardSaveDto);
        boardSaveDto.setBoardId(board.getId());
        // 게시판 요약 생성
        services.boardSummaryTriggerService().triggerSummaryUpdate(boardSaveDto);
        // realtime_keywords에 저장
        services.signalKeywordService().addNewRealtimeKeyword(boardSaveDto, rank);
        // board_rank에 저장
        services.boardRedisService().saveBoardRedis(boardSaveDto, score);
        // 인메모리 캐시에 게시판 정보 갱신
        services.boardCache().addRealtimeBoardCache(boardSaveDto);
        // OpenSearch에 인덱싱
        services.openSearchService.saveKeyword(boardSaveDto.getBoardName());
    }

    /**
     * 실시간 이슈 키워드 변경 SSE 이벤트 발행
     */
    private void publishRealtimeKeywordsEvent(Services services) {
        Top10WithChange top10WithChange = services.signalKeywordService().getRealTimeKeyword();

        Set<String> allClientId = services.signalKeywordService().findAllClientId();
        for (String clientId : allClientId) {
            log.info("스케줄러에서 clientId: {}에게 이벤트 발행", clientId);
            SignalKeywordEventDto event = new SignalKeywordEventDto(clientId,
                    SIGNAL_KEYWORD_LIST_EVENT_MESSAGE, top10WithChange);
            services.redisPublisher().publishSignalKeywordEvent(event);
        }
    }
}
