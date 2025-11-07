package com.trend_now.backend.board.application;

import com.trend_now.backend.board.application.board_summary.BoardSummaryTriggerService;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.repository.BoardRepository;
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
        BoardSummaryTriggerService boardSummaryTriggerService
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
                 *  1. 실시간 게시판에도 없고, 유사한 게시판도 없지만 DB에는 저장 돼 있는 경우 -> DB isDeleted 상태 변경, Redis에 키워드 저장 (복원)
                 *  2. 실시간 게시판에 이미 존재하는 키워드인 경우 -> Redis에 키워드 저장 및 순위 갱신
                 *  3. 똑같은 키워드가 실시간 게시판에 존재하지 않지만, 유사한 게시판은 존재하는 경우 -> DB 게시판 이름 업데이트, Redis에 키워드 저장 및 순위 갱신
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
            applicationContext.getBean(BoardSummaryTriggerService.class)
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
        Optional<Boards> optionalBoards = services.boardRepository()
            .findByName(boardSaveDto.getBoardName());
        double score = calculateScore(now, top10.getRank());

        // DB에 같은 이름의 게시판이 존재하는 경우
        if (optionalBoards.isPresent()) {
            // 케이스 1: 삭제 되었다가 다시 생성된 게시판 복원
            boardSaveDto.setBoardId(optionalBoards.get().getId());
            // 케이스 2: 기존 실시간 게시판 키워드 순위 갱신
            handleExistingBoard(boardSaveDto, top10.getRank(), score, services);
            return;
        }

        // 기존 게시판들과 유사도 검증
        BoardRedisKey boardRedisKey = services.boardCache()
            .findKeywordSimilarity(boardSaveDto.getBoardName());
        String oldBoardName = boardRedisKey.getBoardName();
        boolean hasSimilarBoard = !oldBoardName.equals(boardSaveDto.getBoardName());

        if (hasSimilarBoard) {
            // 케이스 3: 유사한 게시판이 존재하는 경우
            boardSaveDto.setBoardId(boardRedisKey.getBoardId());
            handleSimilarBoard(boardSaveDto, oldBoardName, top10.getRank(), score, services);
            return;
        }

        // 케이스 4: 완전히 새로운 게시판 생성
        handleNewBoard(boardSaveDto, top10.getRank(), score, services);
    }

    /**
     * 케이스 1, 케이스 2
     */
    private void handleExistingBoard(BoardSaveDto boardSaveDto, int rank,
        double score, Services services) {

        boolean isRealTimeBoard = services.boardRedisService().isRealTimeBoard(boardSaveDto);

        if (!isRealTimeBoard) {
            // 케이스 1: 실시간 게시판에 없음 (복원)
            restoreDeletedBoard(boardSaveDto, rank, services);
        } else {
            // 케이스 2: 실시간 게시판에 이미 존재 (순위 갱신)
            updateExistingRealtimeBoard(boardSaveDto, rank, services);
        }

        services.boardCache().addRealtimeBoardCache(boardSaveDto);
        services.boardRedisService().saveBoardRedis(boardSaveDto, score);
    }

    /**
     * 케이스 1: 삭제 되었다가 다시 생성된 게시판 복원
     */
    private void restoreDeletedBoard(BoardSaveDto boardSaveDto, int rank,
        Services services) {

        log.info("삭제 되었다가 다시 생성된 게시판 복원 처리 - boardId: {}, boardName: {}",
            boardSaveDto.getBoardId(), boardSaveDto.getBoardName());

        // Quartz Job에서 조회된 엔티티는 Detached 상태이므로 트랜잭션 내에서 조회 후 수정해야 함
        services.boardService().updateIsDeleted(boardSaveDto.getBoardId(), false);
        // 삭제 되었다가 다시 생성된 게시판이므로 게시판 요약 재생성
        services.boardSummaryTriggerService().triggerSummaryUpdate(boardSaveDto);
        // realtime_keywords에 NEW로 저장
        services.signalKeywordService().addNewRealtimeKeyword(boardSaveDto, rank);
    }

    /**
     * 케이스 2: 기존 실시간 게시판 키워드 순위 갱신
     */
    private void updateExistingRealtimeBoard(BoardSaveDto boardSaveDto, int rank,
        Services services) {

        log.info("기존 실시간 게시판 키워드 순위 갱신 - boardId: {}, boardName: {}",
            boardSaveDto.getBoardId(), boardSaveDto.getBoardName());

        services.signalKeywordService().addRealtimeKeywordWithRankTracking(boardSaveDto.getBoardId(),
            boardSaveDto.getBoardName(), boardSaveDto.getBoardName(), rank);
    }

    /**
     * 케이스 3: 유사한 게시판이 존재하는 경우 (게시판 이름 업데이트)
     */
    private void handleSimilarBoard(BoardSaveDto boardSaveDto,
        String oldBoardName, int rank, double score, Services services) {

        log.info("Redis에서 유사한 게시판 조회 후 업데이트 - 기존 게시판명: {}, 새로운 게시판명: {}",
            oldBoardName, boardSaveDto.getBoardName());

        // 새로운 키워드 랭크 변동 추이 계산 후 realtime_keywords에 저장
        services.signalKeywordService().addRealtimeKeywordWithRankTracking(boardSaveDto.getBoardId(), oldBoardName,
            boardSaveDto.getBoardName(), rank);
        // board_rank에 새로운 키워드 저장
        services.boardRedisService().saveBoardRedis(boardSaveDto, score);
        // 기존 게시판은 board_rank에서 삭제
        services.boardRedisService().deleteKeyInBoardRank(boardSaveDto.getBoardId(), oldBoardName);
        // 게시판 이름 변경
        services.boardService().updateBoardName(boardSaveDto);
        // 캐시 업데이트
        services.boardCache().addRealtimeBoardCache(boardSaveDto);
    }

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
